import asyncio
import unittest
import sys
from loguru import logger

from test.cases.TestCase01_client_send import TestCase01_client_send
from test.cases.TestCase02_auto_reconnect import TestCase02_auto_reconnect
from test.cases.TestCase03_client_session_close import TestCase03_client_session_close
from test.cases.TestCase04_sendAndRequest_timeout import TestCase04_sendAndRequest_timeout
from test.cases.TestCase05_file import TestCase05_file
from test.cases.TestCase06_meta_size import TestCase06_meta_size
from test.cases.TestCase07_url_auth import TestCase07_url_auth
from test.cases.TestCase08_ping_timout import TestCase08_ping_timout
from test.cases.TestCase09_clientCloseReconnect import TestCase09_clientCloseReconnect
from test.modelu.BaseTest import BaseTest


class TestCase01(unittest.TestCase):
    schemas = ["ws"]

    def __init__(self, *args, **kwargs):
        logger.remove()
        logger.add(sys.stderr, level="DEBUG")
        super().__init__(*args, **kwargs)

    def test_Case01_client_send(self):
        for i in range(len(TestCase01.schemas)):
            t = TestCase01_client_send(TestCase01.schemas[i], 9000 + i)
            try:
                t.start()
                t.stop()
            except Exception as e:
                t.on_error()
                raise e

    def test_Case02_auto_reconnect(self):
        for i in range(len(TestCase01.schemas)):
            t = TestCase02_auto_reconnect(TestCase01.schemas[i], 9000 + i)
            try:
                t.start()
                t.stop()
            except Exception as e:
                t.on_error()
                raise e

    def test_Case03_client_session_close(self):
        for i in range(len(TestCase01.schemas)):
            t = TestCase03_client_session_close(TestCase01.schemas[i], 9000 + i)
            try:
                t.start()
                t.stop()
            except Exception as e:
                t.on_error()
                raise e

    def test_Case04sendAndRequest_timeout(self):
        for i in range(len(TestCase01.schemas)):
            t = TestCase04_sendAndRequest_timeout(TestCase01.schemas[i], 9000 + i)
            try:
                t.start()
                t.stop()
            except Exception as e:
                t.on_error()
                raise e

    def test_Case05_file(self):
        for i in range(len(TestCase01.schemas)):
            t = TestCase05_file(TestCase01.schemas[i], 9000 + i)
            try:
                t.start()
                t.stop()
            except Exception as e:
                t.on_error()
                raise e

    def test_Case06_mete_size(self):
        for i in range(len(TestCase01.schemas)):
            t = TestCase06_meta_size(TestCase01.schemas[i], 9000 + i)
            try:
                t.start()
                t.stop()
            except Exception as e:
                t.on_error()
                raise e

    def test_Case07_url_auth(self):
        for i in range(len(TestCase01.schemas)):
            t = TestCase07_url_auth(TestCase01.schemas[i], 9000 + i)
            try:
                t.start()
                t.stop()
            except Exception as e:
                t.on_error()
                raise e

    def test_Case08_ping_timout(self):
        for i in range(len(TestCase01.schemas)):
            t = TestCase08_ping_timout(TestCase01.schemas[i], 9000 + i)
            try:
                t.start()
                t.stop()
            except Exception as e:
                t.on_error()
                raise e

    def test_Case09_clientCloseReconnect(self):
        for i in range(len(TestCase01.schemas)):
            t = TestCase09_clientCloseReconnect(TestCase01.schemas[i], 9000 + i)
            try:
                t.start()
                t.stop()
            except Exception as e:
                t.on_error()
                raise e