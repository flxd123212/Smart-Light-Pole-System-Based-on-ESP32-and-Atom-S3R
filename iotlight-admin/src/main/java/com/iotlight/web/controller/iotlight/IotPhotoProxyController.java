package com.iotlight.web.controller.iotlight;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

/**
 * 摄像头抓拍图片代理
 * 前端请求 /api/photo/xxx.jpg 代理到 Flask 服务器 http://localhost:5000/api/photo/xxx.jpg
 */
@RestController
@RequestMapping("/api/photo")
public class IotPhotoProxyController
{
    private static final String FLASK_BASE = "http://localhost:5000/api/photo/";

    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/{filename}")
    public ResponseEntity<byte[]> getPhoto(@PathVariable String filename)
    {
        try
        {
            byte[] data = restTemplate.getForObject(FLASK_BASE + filename, byte[].class);
            if (data == null || data.length == 0)
            {
                return ResponseEntity.notFound().build();
            }
            HttpHeaders headers = new HttpHeaders();
            if (filename.endsWith(".png"))
            {
                headers.setContentType(MediaType.IMAGE_PNG);
            }
            else
            {
                headers.setContentType(MediaType.IMAGE_JPEG);
            }
            headers.setContentLength(data.length);
            headers.setCacheControl(CacheControl.maxAge(1, java.util.concurrent.TimeUnit.HOURS));
            return new ResponseEntity<>(data, headers, HttpStatus.OK);
        }
        catch (Exception e)
        {
            return ResponseEntity.notFound().build();
        }
    }
}
