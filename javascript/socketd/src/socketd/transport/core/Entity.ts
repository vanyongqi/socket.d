import {StrUtils} from "../../utils/StrUtils";
import {ArrayBufferCodecReader, CodecReader} from "./Codec";
import {Constants, EntityMetas} from "./Constants";
import {BlobBuffer, type Buffer, ByteBuffer} from "./Buffer";
import {SocketdException} from "../../exception/SocketdException";


/**
 * 消息实体（帧[消息[实体]]）
 *
 * @author noear
 * @since 2.0
 */
export interface Entity {
    /**
     * at
     *
     * @since 2.1
     */
    at();

    /**
     * 获取元信息字符串（queryString style）
     */
    metaString(): string;

    /**
     * 获取元信息字典
     */
    metaMap(): URLSearchParams;

    /**
     * 获取元信息
     */
    meta(name: string): string | null;

    /**
     * 获取元信息或默认
     */
    metaOrDefault(name: string, def: string): string;

    /**
     * 获取元信息并转为 int
     */
    metaAsInt(name:string):number;

    /**
     * 获取元信息并转为 float
     */
    metaAsFloat(name:string):number;

    /**
     * 添加元信息
     * */
    putMeta(name: string, val: string);

    /**
     * 获取数据
     */
    data(): Buffer;

    /**
     * 获取数据并转为读取器
     */
    dataAsReader(): CodecReader;

    /**
     * 获取数据并转为字符串
     */
    dataAsString(): string;

    /**
     * 获取数据长度
     */
    dataSize(): number;

    /**
     * 释放资源
     */
    release();
}

/**
 * 答复实体
 *
 * @author noear
 * @since 2.1
 */
export interface Reply extends Entity {
    /**
     * 流Id
     */
    sid(): string;

    /**
     * 是否答复结束
     */
    isEnd(): boolean
}

/**
 * 实体默认实现
 *
 * @author noear
 * @since 2.0
 */
export class EntityDefault implements Entity {
    private _metaMap: URLSearchParams | null;
    private _data: Buffer;
    private _dataAsReader: CodecReader | null;

    constructor() {
        this._metaMap = null;
        this._data = Constants.DEF_DATA;
        this._dataAsReader = null;
    }

    /**
     * At
     * */
    at() {
        return this.meta("@");
    }

    /**
     * 设置元信息字符串
     * */
    metaStringSet(metaString: string): EntityDefault {
        this._metaMap = new URLSearchParams(metaString);
        return this;
    }

    /**
     * 放置元信息字典
     *
     * @param map 元信息字典
     */
    metaMapPut(map:any): EntityDefault {
        if (map instanceof URLSearchParams) {
            const tmp = map as URLSearchParams;
            tmp.forEach((val, key, p) => {
                this.metaMap().set(key, val);
            })
        } else {
            for (const name of map.prototype) {
                this.metaMap().set(name, map[name]);
            }
        }

        return this;
    }

    /**
     * 放置元信息
     *
     * @param name 名字
     * @param val  值
     */
    metaPut(name: string, val: string): EntityDefault {
        this.metaMap().set(name, val);
        return this;
    }

    /**
     * 获取元信息字符串（queryString style）
     */
    metaString(): string {
        return this.metaMap().toString();
    }

    /**
     * 获取元信息字典
     */
    metaMap(): URLSearchParams {
        if (this._metaMap == null) {
            this._metaMap = new URLSearchParams();
        }

        return this._metaMap;
    }

    /**
     * 获取元信息
     *
     * @param name 名字
     */
    meta(name: string): string | null {
        return this.metaMap().get(name);
    }

    /**
     * 获取元信息或默认值
     *
     * @param name 名字
     * @param def  默认值
     */
    metaOrDefault(name: string, def: string): string {
        const val = this.meta(name);
        if (val) {
            return val;
        } else {
            return def;
        }
    }

    /**
     * 获取元信息并转为 int
     */
    metaAsInt(name:string):number {
        return parseInt(this.metaOrDefault(name, '0'));
    }

    /**
     * 获取元信息并转为 float
     */
    metaAsFloat(name:string):number {
        return parseFloat(this.metaOrDefault(name, '0'));
    }

    /**
     * 放置元信息
     *
     * @param name 名字
     * @param val  值
     */
    putMeta(name: string, val: string) {
        this.metaPut(name, val);
    }

    /**
     * 设置数据
     *
     * @param data 数据
     */
    dataSet(data: Blob | ArrayBuffer): EntityDefault {
        if (data instanceof Blob) {
            this._data = new BlobBuffer(data);
        } else {
            this._data = new ByteBuffer(data);
        }

        return this;
    }

    /**
     * 获取数据（若多次复用，需要reset）
     */
    data(): Buffer {
        return this._data;
    }

    dataAsReader(): CodecReader {
        if(this._data.getArray() == null){
            throw new SocketdException("Blob does not support dataAsReader");
        }

        if (!this._dataAsReader) {
            this._dataAsReader = new ArrayBufferCodecReader(this._data.getArray()!);
        }

        return this._dataAsReader;
    }

    /**
     * 获取数据并转成字符串
     */
    dataAsString(): string {
        if (this._data.getArray() == null) {
            throw new SocketdException("Blob does not support dataAsString");
        }

        return StrUtils.bufToStrDo(this._data.getArray()!, '');
    }

    /**
     * 获取数据长度
     */
    dataSize(): number {
        return this._data.size();
    }

    /**
     * 释放资源
     */
    release() {

    }

    toString(): string {
        return "Entity{" +
            "meta='" + this.metaString() + '\'' +
            ", data=byte[" + this.dataSize() + ']' + //避免内容太大，影响打印
            '}';
    }
}

/**
 * 字符串实体
 *
 * @author noear
 * @since 2.0
 */
export class StringEntity extends EntityDefault implements Entity {
    constructor(data: string) {
        super();
        const dataBuf = StrUtils.strToBuf(data);
        this.dataSet(dataBuf);
    }
}

export class FileEntity extends EntityDefault implements Entity {

    constructor(file: File) {
        super();
        this.dataSet(file);
        this.metaPut(EntityMetas.META_DATA_DISPOSITION_FILENAME, file.name);
    }
}
