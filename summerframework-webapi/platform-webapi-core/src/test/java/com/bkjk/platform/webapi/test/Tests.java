package com.bkjk.platform.webapi.test;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class Tests {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    public void testExceptionApiController() throws Exception {
        webTestClient.get().uri("/exception/item/1").exchange().expectStatus().isOk().expectBody()
            .json("{'data':{'name':'item1','price':10}}");
        webTestClient.get().uri("/exception/item/aa").exchange().expectStatus().isOk().expectBody()
            .json("{'code':'1001','message':'商品ID只能是数字'}");
        webTestClient.get().uri("/exception/item/0").exchange().expectStatus().isOk().expectBody()
            .json("{'code':'500','message':'不合法的商品ID'}");
        webTestClient.post().uri("/exception/item").syncBody(new MyItem()).exchange().expectStatus().isOk().expectBody()
            .json("{'code':'422','error':'Unprocessable Entity'}");
        webTestClient.post().uri("/exception/item").syncBody(new MyItem("a", 1)).exchange().expectStatus().isOk()
            .expectBody().json("{'code':'422','error':'Unprocessable Entity'}");
        webTestClient.post().uri("/exception/item").syncBody(new MyItem("a", 10)).exchange().expectStatus().isOk()
            .expectBody().json("{'code':'422','error':'Unprocessable Entity'}");
        webTestClient.post().uri("/exception/item").syncBody(new MyItem("aa", 10)).exchange().expectStatus().isOk()
            .expectBody().json("{'code':'200'}");
    }

    @Test
    public void testFilterApiController() throws Exception {
        webTestClient.get().uri("/filter/health").exchange().expectStatus().isOk().expectBody().json("{'data':'OK'}");
        webTestClient.get().uri("/filter/hello").exchange().expectStatus().isOk().expectBody()
            .json("{'message':'请登录！'}");
        webTestClient.get().uri("/filter/item1").header("token", "1").exchange().expectStatus().isOk().expectBody()
            .json("{'data':{'name':'aatestfilter3testfilter2testfilter1','price':10}}");
        webTestClient.get().uri("/filter/item2").header("token", "1").exchange().expectStatus().isOk().expectBody()
            .json("{'data':{'name':'aatestfilter3testfilter1testfilter2','price':10}}");
        webTestClient.get().uri("/filter/item3").header("token", "1").exchange().expectStatus().isOk().expectBody()
            .json("{'data':{'name':'aa','price':10}}");
    }

    @Test
    public void testMiscApiController() throws Exception {
        webTestClient.post().uri("/misc/item").syncBody(new MyItem("aa", 10)).exchange().expectStatus().isOk()
            .expectBody().json("{'data':{'name':'aa','price':10}}");
        webTestClient.get().uri("swagger-ui.html#").exchange().expectStatus().isOk();
    }

    @Test
    public void testRestController() {
        webTestClient.get().uri("/rest/item").exchange().expectStatus().isOk().expectBody()
            .json("{'name':'aa','price':10}");
        webTestClient.post().uri("/rest/item").syncBody(new MyItem("a", 1)).exchange().expectStatus().isOk()
            .expectBody().json("{'name':null,'price':null}");
        webTestClient.post().uri("/rest/item2").syncBody(new MyItem("a", 1)).exchange().expectStatus().isOk()
            .expectBody().json("{'name':'a','price':1}");
        webTestClient.get().uri("/rest/item/aa").exchange().expectStatus().is5xxServerError();
        webTestClient.get().uri("/rest/item/0").exchange().expectStatus().is5xxServerError();
    }

    @Test
    public void testResultApiController() throws Exception {
        webTestClient.get().uri("/result/test1").exchange().expectStatus().isOk().expectBody().json("{'data':false}");
        webTestClient.get().uri("/result/test2").exchange().expectStatus().isOk().expectBody().json("{'data':0.2}");
        webTestClient.get().uri("/result/test3").exchange().expectStatus().isOk().expectBody()
            .json("{'data':{'name':'aa','price':10}}");
        webTestClient.get().uri("/result/test1").exchange().expectStatus().isOk().expectBody().jsonPath("$.success")
            .exists().jsonPath("$.error").exists().jsonPath("$.code").exists().jsonPath("$.path").exists()
            .jsonPath("$.message").exists().jsonPath("$.data").exists().jsonPath("$.time").exists();
        webTestClient.get().uri("/result/test4").exchange().expectStatus().isOk().expectBody().jsonPath("$.data")
            .doesNotExist();
        webTestClient.get().uri("/result/test5").exchange().expectStatus().isOk().expectBody().jsonPath("$.success")
            .doesNotExist();
    }

    @Test
    public void testVersionApiController() throws Exception {
        webTestClient.get().uri("/v1/ver/hello").exchange().expectStatus().isOk().expectBody()
            .json("{'data':'hello1'}");
        webTestClient.get().uri("/ver/hello").exchange().expectStatus().isOk().expectBody().json("{'data':'hello2'}");
        webTestClient.get().uri("/v2/ver/hello").exchange().expectStatus().isOk().expectBody()
            .json("{'data':'hello3'}");
        webTestClient.get().uri("/v3/ver/hello").exchange().expectStatus().isOk().expectBody()
            .json("{'data':'hello3'}");
        webTestClient.get().uri("/v1/ver/hello/zhuye").exchange().expectStatus().isOk().expectBody()
            .json("{'data':'hello1zhuye'}");
        webTestClient.get().uri("/ver/hello/zhuye").exchange().expectStatus().isOk().expectBody()
            .json("{'data':'hello2zhuye'}");
        webTestClient.get().uri("/v2/ver/hello/zhuye").exchange().expectStatus().isOk().expectBody()
            .json("{'data':'hello3zhuye'}");
        webTestClient.get().uri("/v3/ver/hello/zhuye").exchange().expectStatus().isOk().expectBody()
            .json("{'data':'hello3zhuye'}");
    }
}
