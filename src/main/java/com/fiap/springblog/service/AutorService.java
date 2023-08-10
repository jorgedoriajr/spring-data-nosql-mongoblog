package com.fiap.springblog.service;

import com.fiap.springblog.model.Autor;

public interface AutorService {
    public Autor criar(Autor autor);
    public Autor obterPorCodigo(String codigo);
}
