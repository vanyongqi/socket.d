from abc import ABC, abstractmethod
from typing import Type, TypeVar, Callable

from socketd.transport.core import Buffer
from socketd.transport.core.entity.Frame import Frame

In = TypeVar("In", bound=Type)
Out = TypeVar("Out", bound=Type)


class Codec(ABC):
    """
    编解码器
    """

    @abstractmethod
    def read(self, buffer: Buffer) -> Frame:
        """
        编码
        """
        pass

    @abstractmethod
    def write(self, frame, target: Callable) -> Buffer:
        """
        解码
        """
        pass
