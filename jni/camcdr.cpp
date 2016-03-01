#define LOG_TAG "camcdr"

// 包含头文件
#include <stdlib.h>
#include <stdio.h>
#include <fcntl.h>
#include <pthread.h>
#include <utils/Log.h>
#include "ffjpegdec/ffjpegdec.h"
#include "camcdr.h"

// 内部常量定义
#define CAMCDE_DEF_WIN_PIXFMT  HAL_PIXEL_FORMAT_YV12
#define CAMCDR_DEF_CAM_PIXFMT  V4L2_PIX_FMT_YUYV
#define CAMCDR_DEF_CAM_W       640
#define CAMCDR_DEF_CAM_H       480

// 内部函数实现
static int ALIGN(int x, int y) {
    // y must be a power of 2.
    return (x + y - 1) & ~(y - 1);
}

static void render_rand(void *buf, int size) {
    uint32_t *dst = (uint32_t*)buf;
    while (size > 0) {
        *dst++ = rand();
        size  -= 4;
    }
}

static void* video_render_thread_proc(void *param)
{
    CAMCDR *cam = (CAMCDR*)param;
    int     err;

    while (1) {
        if (cam->thread_state & (1 << 0)) {
            break;
        }

        if (cam->thread_state & (1 << 1)) {
            usleep(10*1000);
            continue;
        }

        if (cam->update_flag) {
            cam->cur_win = cam->new_win;
            if (cam->cur_win != NULL) {
                native_window_set_usage(cam->cur_win.get(),
                    GRALLOC_USAGE_SW_READ_NEVER | GRALLOC_USAGE_SW_WRITE_OFTEN | GRALLOC_USAGE_HW_TEXTURE | GRALLOC_USAGE_EXTERNAL_DISP);
                native_window_set_scaling_mode  (cam->cur_win.get(), NATIVE_WINDOW_SCALING_MODE_SCALE_TO_WINDOW);
                native_window_set_buffer_count  (cam->cur_win.get(), NATIVE_WIN_BUFFER_COUNT);
                native_window_set_buffers_format(cam->cur_win.get(), cam->win_pixfmt);
            }
            cam->update_flag = 0;
        }

        // dequeue camera video buffer
        if (cam->fd > 0) {
            ioctl(cam->fd, VIDIOC_DQBUF, &cam->buf);
        }

//      ALOGD("%d. bytesused: %d, sequence: %d, length = %d\n", cam->buf.index, cam->buf.bytesused,
//              cam->buf.sequence, cam->buf.length);

        {
            int   pts  = (int)(cam->buf.timestamp.tv_usec + cam->buf.timestamp.tv_sec * 1000000);
            char *data = (char*)cam->vbs[cam->buf.index].addr;
            int   len  = cam->buf.bytesused;

            ANativeWindowBuffer *buf;
            if (cam->cur_win != NULL && 0 == native_window_dequeue_buffer_and_wait(cam->cur_win.get(), &buf)) {
                GraphicBufferMapper &mapper = GraphicBufferMapper::get();
                Rect bounds(buf->width, buf->height);
                void *dst = NULL;

                if (0 == mapper.lock(buf->handle, GRALLOC_USAGE_SW_WRITE_OFTEN, bounds, &dst)) {
                    if (len) {
                        ffjpegdec_decode  (cam->jpegdec, data, len, pts);
                        ffjpegdec_getframe(cam->jpegdec, dst, buf->width, buf->height);
                    }
                    else {
                        int dst_y_size   = buf->stride * buf->height;
                        int dst_c_stride = ALIGN(buf->stride / 2, 16);
                        int dst_c_size   = dst_c_stride * buf->height / 2;
                        int dst_buf_size = 0;
                        switch (cam->win_pixfmt) {
                        case HAL_PIXEL_FORMAT_YV12:
                            dst_buf_size = dst_y_size + dst_c_size * 2;
                            break;
                        case HAL_PIXEL_FORMAT_RGB_565:
                            dst_buf_size = dst_y_size * 2;
                            break;
                        case HAL_PIXEL_FORMAT_RGBX_8888:
                            dst_buf_size = dst_y_size * 4;
                            break;
                        }
                        render_rand(dst, dst_buf_size);
                    }
                    mapper.unlock(buf->handle);
                }

                if ((err = cam->cur_win->queueBuffer(cam->cur_win.get(), buf, pts)) != 0) {
                    ALOGW("Surface::queueBuffer returned error %d", err);
                }
            }

            if (!len) {
                usleep(50* 1000);
            }
        }

        // requeue camera video buffer
        if (cam->fd > 0) {
            ioctl(cam->fd, VIDIOC_QBUF , &cam->buf);
        }
    }

    return NULL;
}

// 函数实现
CAMCDR* camcdr_init(const char *dev, int sub, int w, int h)
{
    CAMCDR *cam = (CAMCDR*)malloc(sizeof(CAMCDR));
    if (!cam) {
        return NULL;
    }

    // init context
    memset(cam, 0, sizeof(CAMCDR));
    cam->win_pixfmt= CAMCDE_DEF_WIN_PIXFMT;
    cam->cam_input = sub;
    cam->cam_pixfmt= CAMCDR_DEF_CAM_PIXFMT;
    cam->cam_w     = w ? w : CAMCDR_DEF_CAM_W;
    cam->cam_h     = h ? h : CAMCDR_DEF_CAM_H;

    // open camera device
    cam->fd = open(dev, O_RDWR);
    if (cam->fd < 0) {
        ALOGW("failed to open video device: %s\n", dev);
        goto done;
    }

    struct v4l2_capability cap;
    ioctl(cam->fd, VIDIOC_QUERYCAP, &cap);
    ALOGW("\n");
    ALOGW("video device caps \n");
    ALOGW("------------------\n");
    ALOGW("driver:       %s\n" , cap.driver      );
    ALOGW("card:         %s\n" , cap.card        );
    ALOGW("bus_info:     %s\n" , cap.bus_info    );
    ALOGW("version:      %0x\n", cap.version     );
    ALOGW("capabilities: %0x\n", cap.capabilities);
    ALOGW("\n");

#if 0
    struct v4l2_fmtdesc desc;
    ALOGW("\n");
    ALOGW("VIDIOC_ENUM_FMT   \n");
    ALOGW("------------------\n");
    cam->desc.index = 0;
    cam->desc.type  = V4L2_BUF_TYPE_VIDEO_CAPTURE;
    while (ioctl(cam->fd, VIDIOC_ENUM_FMT, &cam->desc) != -1) {
        ALOGW("%d. flags: %d, description: %-16s, pixelfmt: %d\n",
            cam->desc.index, cam->desc.flags,
            cam->desc.description,
            cam->desc.pixelformat);
        cam->desc.index++;
    }
    ALOGW("\n");
#endif

    if (strcmp((char*)cap.driver, "uvcvideo") != 0) {
        struct v4l2_input input;
        input.index = cam->cam_input;
        ioctl(cam->fd, VIDIOC_S_INPUT, &input);
    }

    cam->fmt.type = V4L2_BUF_TYPE_VIDEO_CAPTURE;
    ioctl(cam->fd, VIDIOC_G_FMT, &cam->fmt);
    cam->fmt.fmt.pix.pixelformat = cam->cam_pixfmt;
    cam->fmt.fmt.pix.width       = cam->cam_w;
    cam->fmt.fmt.pix.height      = cam->cam_h;
    ioctl(cam->fd, VIDIOC_S_FMT, &cam->fmt);
    ALOGW("VIDIOC_G_FMT      \n");
    ALOGW("------------------\n");
    ALOGW("width:        %d\n", cam->fmt.fmt.pix.width       );
    ALOGW("height:       %d\n", cam->fmt.fmt.pix.height      );
    ALOGW("pixfmt:       %d\n", cam->fmt.fmt.pix.pixelformat );
    ALOGW("field:        %d\n", cam->fmt.fmt.pix.field       );
    ALOGW("bytesperline: %d\n", cam->fmt.fmt.pix.bytesperline);
    ALOGW("sizeimage:    %d\n", cam->fmt.fmt.pix.sizeimage   );
    ALOGW("colorspace:   %d\n", cam->fmt.fmt.pix.colorspace  );

    struct v4l2_requestbuffers req;
    req.count  = VIDEO_CAPTURE_BUFFER_COUNT;
    req.type   = V4L2_BUF_TYPE_VIDEO_CAPTURE;
    req.memory = V4L2_MEMORY_MMAP;
    ioctl(cam->fd, VIDIOC_REQBUFS, &req);

    for (int i=0; i<VIDEO_CAPTURE_BUFFER_COUNT; i++)
    {
        cam->buf.type   = V4L2_BUF_TYPE_VIDEO_CAPTURE;
        cam->buf.memory = V4L2_MEMORY_MMAP;
        cam->buf.index  = i;
        ioctl(cam->fd, VIDIOC_QUERYBUF, &cam->buf);

        cam->vbs[i].addr = mmap(NULL, cam->buf.length, PROT_READ | PROT_WRITE, MAP_SHARED,
                                cam->fd, cam->buf.m.offset);
        cam->vbs[i].len  = cam->buf.length;

        ioctl(cam->fd, VIDIOC_QBUF, &cam->buf);
    }

    // init jpeg decoder
    cam->jpegdec = ffjpegdec_init();

done:
    cam->thread_state = (1 << 1);
    pthread_create(&cam->thread_id, NULL, video_render_thread_proc, cam);

    return cam;
}

void camcdr_close(CAMCDR *cam)
{
    int i;

    if (!cam) return;

    // wait thread safely exited
    cam->thread_state |= (1 << 0);
    pthread_join(cam->thread_id, NULL);

    // free jpeg decoder
    ffjpegdec_free(cam->jpegdec);

    // unmap buffers
    for (i=0; i<VIDEO_CAPTURE_BUFFER_COUNT; i++) {
        munmap(cam->vbs[i].addr, cam->vbs[i].len);
    }

    // close & free
    close(cam->fd);
    free(cam);
}

void camcdr_set_preview_window(CAMCDR *cam, const sp<ANativeWindow> win)
{
    if (cam) {
        cam->new_win     = win;
        cam->update_flag = 1;
    }
}

void camcdr_set_preview_target(CAMCDR *cam, const sp<IGraphicBufferProducer>& gbp)
{
    sp<ANativeWindow> win;
    if (gbp != 0) {
        // Using controlledByApp flag to ensure that the buffer queue remains in
        // async mode for the old camera API, where many applications depend
        // on that behavior.
        win = new Surface(gbp, /*controlledByApp*/ true);
    }
    camcdr_set_preview_window(cam, win);
}

void camcdr_start_preview(CAMCDR *cam)
{
    enum v4l2_buf_type type = V4L2_BUF_TYPE_VIDEO_CAPTURE;
    if (cam->fd > 0) {
        ioctl(cam->fd, VIDIOC_STREAMON, &type);
    }

    // start thread
    cam->thread_state &= ~(1 << 1);
}

void camcdr_stop_preview(CAMCDR *cam)
{
    enum v4l2_buf_type type = V4L2_BUF_TYPE_VIDEO_CAPTURE;
    if (cam->fd > 0) {
        ioctl(cam->fd, VIDIOC_STREAMOFF, &type);
    }

    // pause thread
    cam->thread_state |= ~(1 << 1);
}


