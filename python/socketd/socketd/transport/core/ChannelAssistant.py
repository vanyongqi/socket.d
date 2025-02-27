from typing import Any
from abc import ABC, abstractmethod
from socketd.transport.core.entity.Frame import Frame


class ChannelAssistant(ABC):
    @abstractmethod
    async def write(self, target: Any, frame: Frame) -> None:
        pass

    @abstractmethod
    def read(self, buffer: Any) -> Frame: ...

    @abstractmethod
    def is_valid(self, target: Any) -> bool:
        pass

    @abstractmethod
    async def close(self, target: Any) -> None:
        pass

    @abstractmethod
    def get_remote_address(self, target: Any) -> str:
        pass

    @abstractmethod
    def get_local_address(self, target: Any) -> str:
        pass
