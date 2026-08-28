#!/usr/bin/env python3
"""
Tests unitaires de generate-combined-openapi.py (unittest stdlib, aucune dépendance).

Lancement :
    cd docs && python3 -m unittest test_generate_combined_openapi -v
    # ou avec couverture : python3 -m coverage run -m unittest ... && coverage report
"""
import importlib.util
import tempfile
import unittest
from pathlib import Path
from unittest import mock

import yaml

# Le nom du module contient des tirets (generate-combined-openapi.py) :
# import impossible directement, on charge via importlib.
_SPEC_PATH = Path(__file__).resolve().parent / "generate-combined-openapi.py"
_spec = importlib.util.spec_from_file_location("generate_combined_openapi", _SPEC_PATH)
gen = importlib.util.module_from_spec(_spec)
assert _spec.loader is not None
_spec.loader.exec_module(gen)


def write_fixture(directory: Path, name: str, content: dict) -> None:
    (directory / name).write_text(yaml.dump(content), encoding="utf-8")


class LoadTests(unittest.TestCase):

    def test_load_returns_parsed_yaml(self):
        with tempfile.TemporaryDirectory() as tmp:
            write_fixture(Path(tmp), "test-spec.yaml", {"openapi": "3.0.3", "paths": {}})
            with mock.patch.object(gen, "DOCS", Path(tmp)):
                result = gen.load("test-spec.yaml")
        self.assertEqual(result["openapi"], "3.0.3")
        self.assertEqual(result["paths"], {})

    def test_load_raises_on_missing_file(self):
        with tempfile.TemporaryDirectory() as tmp:
            with mock.patch.object(gen, "DOCS", Path(tmp)):
                with self.assertRaises(FileNotFoundError):
                    gen.load("does-not-exist.yaml")


class MainTests(unittest.TestCase):

    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.doc_dir = Path(self.tmp.name)
        self.addCleanup(self.tmp.cleanup)

    def spec(self, title, port_paths, security=None, schemas=None, security_schemes=None):
        return {
            "openapi": "3.0.3",
            "info": {"title": title, "version": "1.0.0"},
            "paths": port_paths,
            "components": {
                "securitySchemes": security_schemes or {},
                "schemas": schemas or {},
            },
            **({"security": security} if security else {}),
        }

    def test_main_merges_paths_and_adds_servers_tags(self):
        write_fixture(self.doc_dir, "package-service-openapi.yaml",
                      self.spec("Pkg", {"/api/packages": {"get": {"summary": "lister"}}}))
        write_fixture(self.doc_dir, "location-service-openapi.yaml",
                      self.spec("Loc", {"/api/locations": {"get": {"summary": "lister"}}}))
        write_fixture(self.doc_dir, "security-checkpoint-service-openapi.yaml",
                      self.spec("Chk", {"/api/checkpoints": {"get": {"summary": "lister"}}}))
        write_fixture(self.doc_dir, "tracking-service-openapi.yaml",
                      self.spec("Trk", {"/api/tracking": {"get": {"summary": "lister"}}}))

        with mock.patch.object(gen, "DOCS", self.doc_dir):
            rc = gen.main()

        self.assertEqual(rc, 0)
        output = yaml.safe_load((self.doc_dir / "combined-openapi.yaml").read_text())
        self.assertEqual(len(output["paths"]), 4)
        # servers par opération : port du service source
        self.assertEqual(output["paths"]["/api/packages"]["get"]["servers"][0]["url"],
                         "http://localhost:8081")
        self.assertEqual(output["paths"]["/api/locations"]["get"]["servers"][0]["url"],
                         "http://localhost:8082")
        self.assertEqual(output["paths"]["/api/checkpoints"]["get"]["servers"][0]["url"],
                         "http://localhost:8083")
        # tags injectés
        self.assertIn("Package Service", output["paths"]["/api/packages"]["get"]["tags"])
        self.assertIn("Location Service", output["paths"]["/api/locations"]["get"]["tags"])

    def test_main_injects_root_security_and_aggregates_schemes(self):
        write_fixture(self.doc_dir, "package-service-openapi.yaml",
                      self.spec("Pkg", {"/api/packages": {"get": {"summary": "s"}}}))
        write_fixture(self.doc_dir, "location-service-openapi.yaml",
                      self.spec("Loc", {"/api/locations": {"get": {"summary": "s"}}}))
        write_fixture(self.doc_dir, "security-checkpoint-service-openapi.yaml",
                      self.spec("Chk", {"/api/checkpoints": {"get": {"summary": "s"}}},
                                security=[{"basicAuth": []}],
                                security_schemes={"basicAuth": {"type": "http", "scheme": "basic"}},
                                schemas={"ProblemDetail": {"type": "object"}}))
        write_fixture(self.doc_dir, "tracking-service-openapi.yaml",
                      self.spec("Trk", {"/api/tracking": {"get": {"summary": "s"}}}))

        with mock.patch.object(gen, "DOCS", self.doc_dir):
            rc = gen.main()

        self.assertEqual(rc, 0)
        output = yaml.safe_load((self.doc_dir / "combined-openapi.yaml").read_text())
        # security root injectée sur l'opération du service qui l'exige
        self.assertEqual(output["paths"]["/api/checkpoints"]["get"]["security"], [{"basicAuth": []}])
        # pas de security sur les autres services
        self.assertNotIn("security", output["paths"]["/api/packages"]["get"])
        # securitySchemes agrégés + schéma importé
        self.assertIn("basicAuth", output["components"]["securitySchemes"])
        self.assertIn("ProblemDetail", output["components"]["schemas"])
        # servers racine (4)
        self.assertEqual(len(output["servers"]), 4)

    def test_main_detects_conflicting_schema(self):
        schema_a = {"type": "object", "properties": {"a": {"type": "string"}}}
        schema_b = {"type": "object", "properties": {"b": {"type": "string"}}}
        write_fixture(self.doc_dir, "package-service-openapi.yaml",
                      self.spec("Pkg", {"/api/packages": {"get": {"summary": "s"}}},
                                schemas={"X": schema_a},
                                security_schemes={"basicAuth": {}}))
        write_fixture(self.doc_dir, "location-service-openapi.yaml",
                      self.spec("Loc", {"/api/locations": {"get": {"summary": "s"}}},
                                schemas={"X": schema_b}))
        write_fixture(self.doc_dir, "security-checkpoint-service-openapi.yaml",
                      self.spec("Chk", {"/api/checkpoints": {"get": {"summary": "s"}}}))
        write_fixture(self.doc_dir, "tracking-service-openapi.yaml",
                      self.spec("Trk", {"/api/tracking": {"get": {"summary": "s"}}}))

        with mock.patch.object(gen, "DOCS", self.doc_dir):
            rc = gen.main()

        self.assertEqual(rc, 1)  # conflit -> code retour 1, pas d'écriture

    def test_main_accepts_identical_schema(self):
        same = {"type": "object", "properties": {"id": {"type": "integer"}}}
        write_fixture(self.doc_dir, "package-service-openapi.yaml",
                      self.spec("Pkg", {"/api/packages": {"get": {"summary": "s"}}}, schemas={"X": same}))
        write_fixture(self.doc_dir, "location-service-openapi.yaml",
                      self.spec("Loc", {"/api/locations": {"get": {"summary": "s"}}}, schemas={"X": same}))
        write_fixture(self.doc_dir, "security-checkpoint-service-openapi.yaml",
                      self.spec("Chk", {"/api/checkpoints": {"get": {"summary": "s"}}}))
        write_fixture(self.doc_dir, "tracking-service-openapi.yaml",
                      self.spec("Trk", {"/api/tracking": {"get": {"summary": "s"}}}))

        with mock.patch.object(gen, "DOCS", self.doc_dir):
            rc = gen.main()

        self.assertEqual(rc, 0)
        output = yaml.safe_load((self.doc_dir / "combined-openapi.yaml").read_text())
        self.assertEqual(output["components"]["schemas"]["X"], same)

    def test_main_merges_duplicate_paths_across_services(self):
        write_fixture(self.doc_dir, "package-service-openapi.yaml",
                      self.spec("Pkg", {"/api/shared": {"get": {"summary": "pkg-get"}}}))
        write_fixture(self.doc_dir, "location-service-openapi.yaml",
                      self.spec("Loc", {"/api/shared": {"post": {"summary": "loc-post"}}}))
        write_fixture(self.doc_dir, "security-checkpoint-service-openapi.yaml",
                      self.spec("Chk", {"/api/checkpoints": {"get": {"summary": "s"}}}))
        write_fixture(self.doc_dir, "tracking-service-openapi.yaml",
                      self.spec("Trk", {"/api/tracking": {"get": {"summary": "s"}}}))

        with mock.patch.object(gen, "DOCS", self.doc_dir), \
                mock.patch("builtins.print") as mock_print:
            rc = gen.main()

        self.assertEqual(rc, 0)
        output = yaml.safe_load((self.doc_dir / "combined-openapi.yaml").read_text())
        self.assertIn("get", output["paths"]["/api/shared"])
        self.assertIn("post", output["paths"]["/api/shared"])
        # avertissement émis pour le path dupliqué
        self.assertTrue(any("dupliqué" in str(call) for call in mock_print.call_args_list))


if __name__ == "__main__":
    unittest.main()
