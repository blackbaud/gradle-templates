package com.blackbaud.service.resources;

import com.blackbaud.service.api.ResourcePaths;
import com.blackbaud.service.api.resource;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = ResourcePaths.RESOURCE_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
public class resourceResource {

    @GetMapping("/{id}")
    public resource find(@PathVariable("id") UUID id) {
        throw new IllegalStateException("implement");
    }


}
