package com.fiap.springblog.repository;

import com.fiap.springblog.model.Autor;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AutorRepository extends MongoRepository<Autor, String> {

}
