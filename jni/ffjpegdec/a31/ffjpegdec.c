// 包含头文件
#include <ffjpegdec.h>
#include "LibveDecoder.h"

// 函数实现
void* ffjpegdec_init(void)
{
    return libveInit(CEDARV_STREAM_FORMAT_MJPEG);
}

void ffjpegdec_decode(void *decoder, void *buf, int len, int pts)
{
    libveDecode((DecodeHandle*)decoder, buf, len, pts, NULL);
}

void ffjpegdec_getframe(void *decoder, void *buf, int w, int h)
{
    libveGetFrame((DecodeHandle*)decoder, buf, w, h, NULL);
}

void ffjpegdec_free(void *decoder)
{
    libveExit((DecodeHandle*)decoder);
}

