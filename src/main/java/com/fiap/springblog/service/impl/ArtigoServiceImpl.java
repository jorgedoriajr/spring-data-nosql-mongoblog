package com.fiap.springblog.service.impl;

import com.fiap.springblog.model.Artigo;
import com.fiap.springblog.model.ArtigoStatusCount;
import com.fiap.springblog.model.Autor;
import com.fiap.springblog.model.AutorTotalArtigo;
import com.fiap.springblog.repository.ArtigoRepository;
import com.fiap.springblog.repository.AutorRepository;
import com.fiap.springblog.service.ArtigoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.TypedAggregation;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ArtigoServiceImpl implements ArtigoService {

    private final MongoTemplate mongoTemplate;

    public ArtigoServiceImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Autowired
    private MongoTransactionManager transactionManager;

    @Autowired
    private ArtigoRepository artigoRepository;

    @Autowired
    private AutorRepository autorRepository;

    @Override
    public List<Artigo> obterTodos() {
        return this.artigoRepository.findAll();
    }

    @Transactional(readOnly = true)
    @Override
    public Artigo obterPorCodigo(String codigo) {
        return this.artigoRepository
                .findById(codigo)
                .orElseThrow(()-> new IllegalArgumentException("Artigo não existe!"));
    }

    @Override
    public ResponseEntity<?> criarArtigoComAutor(Artigo artigo, Autor autor) {

        TransactionTemplate transactionTemplate = new
                TransactionTemplate(transactionManager);

        transactionTemplate.execute(status -> {

            try {

                //Iniciar a transação
                autorRepository.save(autor);
                artigo.setData(LocalDateTime.now());
                artigo.setAutor(autor);
                artigoRepository.save(artigo);

            } catch (Exception e) {
                //Tratar o erro e desfazer a transação
                status.setRollbackOnly();
                throw new RuntimeException("Erro ao criar o artigo com autor: " + e.getMessage());
            }
            return null;
        });
        return null;
    }

    @Override
    public void excluirArtigoEAutor(Artigo artigo) {

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        transactionTemplate.execute(status ->{

            try {

                //Iniciar a transação
                artigoRepository.delete(artigo);
                Autor autor = artigo.getAutor();
                autorRepository.delete(autor);

            } catch (Exception e){

                //Tratamento da transação
                status.setRollbackOnly();
                throw new RuntimeException
                        ("Erro ao excluir artigo e autor: " + e.getMessage());
            }
            return null;
        });
    }

    /*@Override
    public ResponseEntity<?> criar(Artigo artigo) {

        if (artigo.getAutor().getCodigo() != null) {

            //Recupera um Autor existente na lista
            //de documentos pelo seu código
            Autor autor = this.autorRepository
                    .findById(artigo.getAutor().getCodigo())
                    .orElseThrow(() ->
                            new IllegalArgumentException("Autor inexistente!"));

            //Define o autor do artigo
            artigo.setAutor(autor);

        } else {

            //Senão, não atribui um autor ao artigo
            artigo.setAutor(null);
        }

        try {

            //Salva e retorna o artigo cadastrado com
            //seu respectivo autor ou não.
            this.artigoRepository.save(artigo);
            return ResponseEntity.status(HttpStatus.CREATED).build();

        } catch (DuplicateKeyException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Artigo já existe na coleção!");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao criar artigo: " + e.getMessage());
        }

    }*/

    @Override
    public ResponseEntity<?> atualizarArtigo(String id, Artigo artigo) {
        try {

            Artigo existenteArtigo =
                    this.artigoRepository.findById(id).orElse(null);

            if (existenteArtigo == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Artigo não encontrado na coleção!");
            }

            //Atualizar alguns dados do artigo existente
            existenteArtigo.setTitulo(artigo.getTitulo());
            existenteArtigo.setData(artigo.getData());
            existenteArtigo.setTexto(artigo.getTexto());
            this.artigoRepository.save(existenteArtigo);
            return ResponseEntity.status(HttpStatus.OK).build();

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao atualizar artigo: " + e.getMessage());
        }
    }

    /*@Transactional
    @Override
    public Artigo criar(Artigo artigo) {

        //Se o autor existe
        if (artigo.getAutor().getCodigo() != null){

            //recuperar o autor
            Autor autor = this.autorRepository
                    .findById(artigo.getAutor().getCodigo())
                    .orElseThrow(()-> new IllegalArgumentException("Autor inexistente!"));

            //Define o autor no artigo
            artigo.setAutor(autor);

        }else {

            //Caso contrário, gravar o artigo sem autor
            artigo.setAutor(null);
        }

        try {
            //Salvar o artigo com o autor já cadastrado
            return this.artigoRepository.save(artigo);
        } catch (OptimisticLockingFailureException ex){

            //desenvover a estratégia

            //1. Recuperar o documento mais recente do banco de dados (na coleção Artigo)
            Artigo atualizado =
                    artigoRepository.findById(artigo.getCodigo()).orElse(null);

            if (atualizado != null) {

                //2. Atualizar os campos desejados
                atualizado.setTitulo(artigo.getTitulo());
                atualizado.setTexto(artigo.getTexto());
                atualizado.setStatus(artigo.getStatus());

                //3. Incrementar a versão manualmente do documento
                atualizado.setVersion(atualizado.getVersion() + 1);

                //Tentar salvar novamente
                return this.artigoRepository.save(artigo);
            } else {

                //Documento não encontrado, tratar o erro adequadamente
                throw new RuntimeException(
                        "Artigo não encontrado: " + artigo.getCodigo()
                );
            }
        }
    }*/

    @Override
    public List<Artigo> findByDataGreaterThan(LocalDateTime data) {
        Query query = new Query(Criteria.where("data").gt(data));
        return mongoTemplate.find(query, Artigo.class);
    }

    @Override
    public List<Artigo> findByDataAndStatus(LocalDateTime data, Integer status) {
        Query query = new Query(Criteria.where("data")
                .is(data).and("status").is(status));
        return mongoTemplate.find(query, Artigo.class);
    }

    @Transactional
    @Override
    public void atualizar(Artigo updateArtigo) {
        this.artigoRepository.save(updateArtigo);
    }

    @Transactional
    @Override
    public void atualizarArtigo(String id, String novaURL) {
        //Critério de busca pelo "_id"
        Query query = new Query(Criteria.where("_id").is(id));
        //Definindo os campos que serão atualizados
        Update update = new Update().set("url", novaURL);
        //Executo a atualização
        this.mongoTemplate.updateFirst(query, update, Artigo.class);
    }

    @Transactional
    @Override
    public void deleteById(String id) {
        this.artigoRepository.deleteById(id);
    }

    @Override
    public void deleteArtigoById(String id) {
        Query query = new Query(Criteria.where("_id").is(id));
        mongoTemplate.remove(query, Artigo.class);
    }

    @Override
    public List<Artigo> findByStatusAndDataGreaterThan(Integer Status, LocalDateTime data) {
        return this.artigoRepository
                .findByStatusAndDataGreaterThan(Status, data);
    }

    @Override
    public List<Artigo> obterArtigoPorDataHora(LocalDateTime de, LocalDateTime ate) {
        return this.artigoRepository.obterArtigoPorDataHora(de, ate);
    }

    @Override
    public List<Artigo> encontrarArtigosComplexos(Integer status,
                                                  LocalDateTime data,
                                                  String titulo) {
        Criteria criteria = new Criteria();

        // Filtrar artigos com data menor ou igual ao valor fornecido
        criteria.and("data").lte(data);

        // Filtrar artigos com o status especificado
        if (status != null){
            criteria.and("Status").is(status);
        }

        // Filtrar artigos cujo titulo exista
        if (titulo != null && !titulo.isEmpty()){
            criteria.and("titulo").regex(titulo, "i");
        }
        Query query = new Query(criteria);
        return mongoTemplate.find(query, Artigo.class);
    }

    @Override
    public Page<Artigo> listaArtigos(Pageable pageable) {
        Sort sort = Sort.by("titulo").ascending();
        Pageable paginacao =
                PageRequest.of(pageable.getPageNumber(),
                               pageable.getPageSize(), sort);
        return this.artigoRepository.findAll(paginacao);
    }

    @Override
    public List<Artigo> findByStatusOrderByTituloAsc(Integer Status) {
        return this.artigoRepository.findByStatusOrderByTituloAsc(Status);
    }

    @Override
    public List<Artigo> obterArtigoPorStatusComOrdenacao(Integer Status) {
        return this.artigoRepository.obterArtigoPorStatusComOrdenacao(Status);
    }

    @Override
    public List<Artigo> findByTexto(String searchTerm) {
        TextCriteria criteria =
                TextCriteria.forDefaultLanguage().matchingPhrase(searchTerm);
        Query query = TextQuery.queryText(criteria).sortByScore();
        return mongoTemplate.find(query, Artigo.class);
    }

    @Override
    public List<ArtigoStatusCount> contarArtigosPorStatus() {
        TypedAggregation<Artigo> aggregation =
                Aggregation.newAggregation(
                        Artigo.class,
                        Aggregation.group("status").count().as("quantidade"),
                        Aggregation.project("quantidade").and("status")
                                .previousOperation()
                );

        AggregationResults<ArtigoStatusCount> result =
                mongoTemplate.aggregate(aggregation, ArtigoStatusCount.class);

        return result.getMappedResults();
    }

    @Override
    public List<AutorTotalArtigo> calcularTotalArtigosPorAutorNoPeriodo(LocalDate dataInicio,
                                                                        LocalDate dataFim) {
        TypedAggregation<Artigo> aggregation =
                Aggregation.newAggregation(
                        Artigo.class,
                        Aggregation.match(
                                Criteria.where("data")
                                        .gte(dataInicio.atStartOfDay())
                                        .lt(dataFim.plusDays(1).atStartOfDay())

                        ),
                        Aggregation.group("autor").count().as("totalArtigos"),
                        Aggregation.project("totalArtigos").and("autor")
                                .previousOperation()
                );

        AggregationResults<AutorTotalArtigo> results =
                mongoTemplate.aggregate(aggregation, AutorTotalArtigo.class);

        return results.getMappedResults();
    }
}
