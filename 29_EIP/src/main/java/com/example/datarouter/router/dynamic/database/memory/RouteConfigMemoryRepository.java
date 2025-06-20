package com.example.datarouter.router.dynamic.database.memory;

import com.example.datarouter.router.dynamic.database.RouteConfigRepository;
import org.springframework.stereotype.Component;

@Component
public class RouteConfigMemoryRepository implements RouteConfigRepository {

    @Override
    public String findByHeaderName(String headerName) {
        return "processingChannel";
    }
}
