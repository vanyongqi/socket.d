from socketd.transport.core.Costants import Flag
from socketd.transport.core import SocketD
from socketd.transport.core.entity.EntityDefault import EntityDefault
from socketd.transport.core.entity.Frame import Frame
from socketd.transport.core.entity.MessageDefault import MessageDefault


class Frames:

    @staticmethod
    def connectFrame(sid, url):
        entity = EntityDefault()
        entity.put_meta("META_SOCKETD_VERSION", SocketD.SocketD.version())
        return Frame(Flag.Connect, MessageDefault().set_sid(sid).set_event(url).set_entity(entity))

    @staticmethod
    def connackFrame(connectMessage):
        entity = EntityDefault()
        entity.put_meta("META_SOCKETD_VERSION", SocketD.SocketD.version())
        return Frame(Flag.Connack, MessageDefault().set_sid(connectMessage.sid).set_event(
            connectMessage.event).set_entity(entity))

    @staticmethod
    def pingFrame():
        return Frame(Flag.Ping, None)

    @staticmethod
    def pongFrame():
        return Frame(Flag.Pong, None)

    @staticmethod
    def closeFrame():
        return Frame(Flag.Close, None)