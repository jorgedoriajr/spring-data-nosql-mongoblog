package com.fiap.springblog.model;

import lombok.Data;

@Data
public class ArtigoComAutorRequest {

    private Artigo artigo;
    private Autor autor;

}