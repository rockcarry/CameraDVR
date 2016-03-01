// 包含头文件
#include <stdlib.h>
#include <stdio.h>
#include <errno.h>
#include <jpeglib.h>
#include <ffjpegdec.h>

// 内部常量定义
#define DO_USE_VAR(v) do { v = v; } while (0)

// 内部函数实现
static void ffjpegdec_error_exit(j_common_ptr cinfo) {
    jpeg_finish_decompress((j_decompress_ptr)cinfo);
}

static void bufsrc_init_source(j_decompress_ptr cinfo) { DO_USE_VAR(cinfo); }
static void bufsrc_term_source(j_decompress_ptr cinfo) { DO_USE_VAR(cinfo); }
static boolean bufsrc_fill_input_buffer(j_decompress_ptr cinfo) { DO_USE_VAR(cinfo); return TRUE; }
static void bufsrc_skip_input_data(j_decompress_ptr cinfo, long num_bytes)
{
    cinfo->src->next_input_byte += (size_t) num_bytes;
    cinfo->src->bytes_in_buffer -= (size_t) num_bytes;
}

static void jpeg_buffer_src(j_decompress_ptr cinfo, void *buf, int size)
{
    struct jpeg_source_mgr *src;

    if (cinfo->src == NULL) { /* first time for this JPEG object? */
        cinfo->src = (struct jpeg_source_mgr *)
            (*cinfo->mem->alloc_small) ((j_common_ptr) cinfo, JPOOL_PERMANENT, sizeof(struct jpeg_source_mgr));
    }

    src = (struct jpeg_source_mgr *) cinfo->src;
    src->init_source = bufsrc_init_source;
    src->fill_input_buffer = bufsrc_fill_input_buffer;
    src->skip_input_data = bufsrc_skip_input_data;
    src->resync_to_restart = jpeg_resync_to_restart; /* use default method */
    src->term_source = bufsrc_term_source;
    src->bytes_in_buffer = size;
    src->next_input_byte = buf;
}

typedef struct {
    struct jpeg_decompress_struct cinfo;
    struct jpeg_error_mgr jerr;
} CONTEXT;

// 函数实现
void* ffjpegdec_init(void)
{
    CONTEXT *context = malloc(sizeof(CONTEXT));
    if (!context) {
        return NULL;
    }
    else {
        memset(context, 0, sizeof(CONTEXT));
    }

    context->jerr.error_exit = ffjpegdec_error_exit;
    context->cinfo.err = jpeg_std_error(&context->jerr);
    jpeg_create_decompress(&context->cinfo);
    return context;
}

void ffjpegdec_decode(void *decoder, void *buf, int len, int pts)
{
    CONTEXT *context = (CONTEXT*)decoder;
    jpeg_buffer_src(&context->cinfo, buf, len);
    jpeg_read_header(&context->cinfo, TRUE);
    if (0) {
        context->cinfo.out_color_space = JCS_YCbCr;
        context->cinfo.raw_data_out = TRUE;
        context->cinfo.do_fancy_upsampling = FALSE;
    }
    else {
        context->cinfo.out_color_space = JCS_RGBA_8888;
    }
    jpeg_start_decompress(&context->cinfo);
    DO_USE_VAR(pts);
}

void ffjpegdec_getframe(void *decoder, void *buf, int w, int h)
{
    CONTEXT *context = (CONTEXT*)decoder;

    int row_stride = context->cinfo.output_width * context->cinfo.output_components;
    uint8_t *dst_buf = (uint8_t*)buf;
    JSAMPROW dst_row;

    if (w == (int)context->cinfo.output_width && h == (int)context->cinfo.output_height) {
        while (context->cinfo.output_scanline < context->cinfo.output_height) {
            dst_row = (uint8_t*)dst_buf;
            jpeg_read_scanlines(&context->cinfo, &dst_row, 1);
            dst_buf += row_stride;
        }
    }
    else {
        // todo...
    }

    jpeg_finish_decompress(&context->cinfo);
}

void ffjpegdec_free(void *decoder)
{
    CONTEXT *context = (CONTEXT*)decoder;
    jpeg_destroy_decompress((struct jpeg_decompress_struct*)decoder);
    free(context);
}


