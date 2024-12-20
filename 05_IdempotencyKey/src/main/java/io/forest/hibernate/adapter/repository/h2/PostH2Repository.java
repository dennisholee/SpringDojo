package io.forest.hibernate.adapter.repository.h2;

import java.util.UUID;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;

import io.forest.hibernate.adapter.repository.entity.PostEntity;

public interface PostH2Repository extends R2dbcRepository<PostEntity, UUID>, ReactiveQueryByExampleExecutor<PostEntity> {

}
