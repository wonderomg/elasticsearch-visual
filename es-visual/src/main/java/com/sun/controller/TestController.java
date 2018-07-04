package com.sun.controller;

import com.alibaba.fastjson.JSONObject;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchScrollRequestBuilder;
import org.elasticsearch.search.SearchHit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.elasticsearch.action.admin.indices.analyze.AnalyzeAction;
import org.elasticsearch.action.admin.indices.analyze.AnalyzeRequestBuilder;
import org.elasticsearch.action.admin.indices.analyze.AnalyzeResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 类功能描述:
 *
 * @author liu
 * @since 2018/2/22
 */
@RestController
public class TestController {
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    public final static String HOST = "10.10.13.234";

    /** http请求的端口是9200，客户端是9300 */
    public final static int PORT = 9300;

    /** 索引库名 */
    private static String index = "my_index";
    /** 类型名称 */
    private static String type = "skuId";
    private static String clusterName = "my-application";

    private static final String template = "Hello, %s!";
    private final AtomicLong counter = new AtomicLong();

    @ResponseBody
    @RequestMapping("/greeting")
    public Greeting greeting(@RequestParam(value="name", defaultValue="World") String name) {
        return new Greeting(counter.incrementAndGet(),
                String.format(template, name));
    }

    public long getTotalRows(String keyword, TransportClient client) {
        long totalRows;
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        boolQueryBuilder.must(QueryBuilders.termQuery("isShow", 1))
                .must(QueryBuilders.termQuery("status", 1))
                .must(QueryBuilders.termQuery("deleteStatus", 1))
                .must(QueryBuilders.termQuery("salesStatus", 1));
        if (keyword == null || keyword.isEmpty()) {
            totalRows = client.prepareCount(index).setTypes(type)
                    .execute()
                    .actionGet().getCount();
        } else {
            QueryBuilder fBuilder = QueryBuilders.queryStringQuery(keyword).field("name");
            boolQueryBuilder.must(fBuilder);
            totalRows = client.prepareCount(index).setTypes(type)
                    .setQuery(boolQueryBuilder)
                    .execute()
                    .actionGet().getCount();
        }
        return totalRows;
    }

    @ResponseBody
    @RequestMapping("/searchFromSize")
    public Object testSearchByFromSize(@RequestParam(required = false) Integer pageIndex, @RequestParam(required = false) Integer pageSize,
                               @RequestParam(required = false) String keyword, @RequestParam(required = false) String priceSort){
        logger.info("op<testSearchByFromSize> pageIndex<"+pageIndex+"> pageSize<"+pageSize+"> " +
                "keyword<"+keyword+"> priceSort<"+priceSort+">");
        JSONObject result = new JSONObject();
        JSONObject goodsResult = new JSONObject();
        long begin = System.currentTimeMillis();
        TransportClient client = getClient();
        long totalRows = getTotalRows(keyword, client);
        getIkAnalyzeSearchTerms(client, keyword);
        List<Map<String, Object>> goodsExportList = testQueryStringQueryFromSize(client, index, type, "name",
                keyword, pageIndex, pageSize, priceSort);
        goodsResult.put( "goods", goodsExportList );
        goodsResult.put( "pageIndex", pageIndex );
        goodsResult.put( "pageSize", pageSize );
        goodsResult.put( "totalRows", totalRows );

        result.put( "success", true );
        result.put( "resultMessage", "" );
        result.put( "resultCode", "0000" );
        result.put( "result", goodsResult );
        System.out.println("---op<searchFromSize> elapsed<"+(System.currentTimeMillis()-begin)+">ms");
        return result;
    }

    public String scrollIdTemp = "";

    @ResponseBody
    @RequestMapping("/searchScroll")
    public Object testSearchByScroll(@RequestParam(required = false) String scrollId, @RequestParam(required = false) Integer pageSize,
                               @RequestParam(required = false) String keyword, @RequestParam(required = false) String priceSort){
        logger.info("op<testSearchByScroll> scrollId<"+scrollId+"> pageSize<"+pageSize+"> " +
                "keyword<"+keyword+"> priceSort<"+priceSort+">");
        JSONObject result = new JSONObject();
        JSONObject goodsResult = new JSONObject();
        long begin = System.currentTimeMillis();
        scrollIdTemp = scrollId;
        TransportClient client = getClient();
        long totalRows = getTotalRows(keyword, client);
        getIkAnalyzeSearchTerms(client, keyword);
        List<Map<String, Object>> goodsExportList = testQueryStringQueryScroll(client, index, type, "name",
                keyword, scrollIdTemp, pageSize, priceSort);
        client.close();

        goodsResult.put( "goods", goodsExportList );
        goodsResult.put( "scrollId", scrollIdTemp );
        goodsResult.put( "pageSize", pageSize );
        goodsResult.put( "totalRows", totalRows );

        result.put( "success", true );
        result.put( "resultMessage", "" );
        result.put( "resultCode", "0000" );
        result.put( "result", goodsResult );
        System.out.println("---op<searchScroll> elapsed<"+(System.currentTimeMillis()-begin)+">ms");
        return result;
    }

    /**
     * 然后就是创建两个查询过程了 ，下面是from-size分页的执行代码：
     */
    public List testQueryStringQueryFromSize(TransportClient client, String index, String type, String key,
                                     String value, Integer pageIndex, Integer pageSize ,String priceSort){
        SearchResponse response = null;
        List resultList = null;
        try {
            //TransportClient client = getClient();
            BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
            if (value == null || value.isEmpty()) {
                boolQueryBuilder.must(QueryBuilders.termQuery("isShow", 1))
                        .must(QueryBuilders.termQuery("status", 1))
                        .must(QueryBuilders.termQuery("deleteStatus", 1))
                        .must(QueryBuilders.termQuery("salesStatus", 1));
            } else {
                QueryBuilder fBuilder = QueryBuilders.queryStringQuery(value).field(key);
                boolQueryBuilder.must(QueryBuilders.termQuery("isShow", 1))
                        .must(QueryBuilders.termQuery("status", 1))
                        .must(QueryBuilders.termQuery("deleteStatus", 1))
                        .must(QueryBuilders.termQuery("salesStatus", 1))
                        .must(fBuilder);
            }

            if (priceSort == null || priceSort.isEmpty()) {
                response = client.prepareSearch(index).setTypes(type)
                        .setQuery(boolQueryBuilder)
                        .setFrom(pageIndex).setSize(pageSize).setExplain(true)
                        .execute()
                        .actionGet();
            } else {
                if (("desc").equals(priceSort)) {
                    response = client.prepareSearch(index).setTypes(type)
                            .setQuery(boolQueryBuilder)
                            .setFrom(pageIndex).setSize(pageSize).setExplain(true)
                            .addSort("price", SortOrder.DESC)
                            .execute()
                            .actionGet();
                } else {
                    response = client.prepareSearch(index).setTypes(type)
                            .setQuery(boolQueryBuilder)
                            .setFrom(pageIndex).setSize(pageSize).setExplain(true)
                            .addSort("price", SortOrder.ASC)
                            .execute()
                            .actionGet();
                }
            }
            resultList = responseToList(response);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return resultList;
    }
    /**
     * 下面是scroll分页的执行代码，注意啊！scroll里面的size是相对于每个分片来说的，
     * 所以实际返回的数量是：分片的数量*size
     *
     * Scroll-Scan 方式与普通 scroll 有几点不同：
     * 1.Scroll-Scan 结果没有排序，按 index 顺序返回，没有排序，可以提高取数据性能。
     * 2.初始化时只返回 _scroll_id，没有具体的 hits 结果。
     * 3.size 控制的是每个分片的返回的数据量而不是整个请求返回的数据量。
     */
    public List testQueryStringQueryScroll(TransportClient client, String index, String type, String key,
                                     String value, String scrollId, Integer pageSize ,String priceSort){
        if (pageSize != null) {
            //默认有5个分片，
            pageSize = pageSize/5;
        }
        SearchResponse scrollResponse = null;
        List resultList = null;
        try {
            //TransportClient client = getClient();
            BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
            if (value == null || value.isEmpty()) {
                boolQueryBuilder.must(QueryBuilders.termQuery("isShow", 1))
                        .must(QueryBuilders.termQuery("status", 1))
                        .must(QueryBuilders.termQuery("deleteStatus", 1))
                        .must(QueryBuilders.termQuery("salesStatus", 1));
            } else {
                QueryBuilder fBuilder = QueryBuilders.queryStringQuery(value).field(key);
                boolQueryBuilder.must(QueryBuilders.termQuery("isShow", 1))
                        .must(QueryBuilders.termQuery("status", 1))
                        .must(QueryBuilders.termQuery("deleteStatus", 1))
                        .must(QueryBuilders.termQuery("salesStatus", 1))
                        .must(fBuilder);
            }

            if (scrollId == null || scrollId.isEmpty()) {
                if (priceSort == null || priceSort.isEmpty()) {
                    scrollResponse = client.prepareSearch(index).setTypes(type)
                            .setQuery(boolQueryBuilder)
                            .setSearchType(SearchType.SCAN).setSize(pageSize).setScroll(TimeValue.timeValueMinutes(5))
                            .execute()
                            .actionGet();
                } else {
                    if (("desc").equals(priceSort)) {
                        scrollResponse = client.prepareSearch(index).setTypes(type)
                                .setQuery(boolQueryBuilder)
                                .setSearchType(SearchType.SCAN).setSize(pageSize).setScroll(TimeValue.timeValueMinutes(5))
                                .addSort("price", SortOrder.DESC)
                                .execute()
                                .actionGet();
                    } else {
                        scrollResponse = client.prepareSearch(index).setTypes(type)
                                .setQuery(boolQueryBuilder)
                                .setSearchType(SearchType.SCAN).setSize(pageSize).setScroll(TimeValue.timeValueMinutes(5))
                                .addSort("price", SortOrder.ASC)
                                .execute()
                                .actionGet();
                    }
                }
                //注意:首次搜索并不包含数据，第一次不返回数据，所以先查一次获取scrollId，在进行第二次scroll查询
                scrollId = scrollResponse.getScrollId();
            }

            scrollResponse = client.prepareSearchScroll(scrollId)
                    .setScroll(TimeValue.timeValueMinutes(5))
                    .execute()
                    .actionGet();
            
            //注意:首次搜索并不包含数据，第一次不返回数据，获取总数量
            long count = scrollResponse.getHits().getTotalHits();
            System.out.println("scrollResponse.getHits().getHits().length:"+scrollResponse.getHits().hits().length);
            System.out.println("scroll count:"+count);

            //获取本次查询的scrollId
            scrollIdTemp = scrollResponse.getScrollId();
            System.out.println("--------- searchByScroll scrollID:"+scrollIdTemp);
            logger.info("--------- searchByScroll scrollID {}", scrollIdTemp);

            // 搜索结果
            SearchHit[] searchHits = scrollResponse.getHits().hits();
            for (SearchHit searchHit : searchHits) {
                String source = searchHit.getSource().toString();
                logger.info("--------- searchByScroll source {}", source);
            }
            resultList = responseToList(scrollResponse);
        } catch (Exception e) {
            e.printStackTrace();
        }


        return resultList;
    }

    /**
     * 将查询后获得的response转成list
     * @param response
     * @return
     */
    public List responseToList(SearchResponse response){
        SearchHits hits = response.getHits();
        List<Map<String, Object>> list = new ArrayList<>();
        System.out.println("hits.hits().length:"+hits.hits().length);
        System.out.println("------------------------");
        for (int i = 0; i < hits.hits().length; i++) {
            Map<String, Object> map = hits.getAt(i).getSource();
            Map<String, Object> resultMap = new HashMap<>(16);
            System.out.println(map.get("name"));
            resultMap.put("skuId", map.get("skuId"));
            resultMap.put("name", map.get("name"));
            resultMap.put("image", map.get("primaryImage"));
            resultMap.put("price", map.get("luckyPrice"));
            list.add(resultMap);
        }
        System.out.println("------------------------");
        return list;
    }

    public TransportClient getClient() {
        /**
         * 1:通过 setting对象来指定集群配置信息
         * //指定集群名称
         * //启动嗅探功能
         */
        Settings settings = Settings.settingsBuilder()
                .put("cluster.name", clusterName)
                .put("client.transport.sniff", true)
                .build();

        /**
         * 2：创建客户端
         * 通过setting来创建，若不指定则默认链接的集群名为elasticsearch
         * 链接使用tcp协议即9300
         */
        TransportClient client = null;
        try {
            client = TransportClient.builder().settings(settings).build()
                    .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(HOST), PORT));
            System.out.println("Elasticsearch connect info:" + client.toString());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        /*transportClient = new TransportClient(setting);
        TransportAddress transportAddress = new InetSocketTransportAddress("192.168.79.131", 9300);
        transportClient.addTransportAddresses(transportAddress);*/

        /**
         * 3：查看集群信息
         * 注意我的集群结构是：
         *   131的elasticsearch.yml中指定为主节点不能存储数据，
         *   128的elasticsearch.yml中指定不为主节点只能存储数据。
         * 所有控制台只打印了192.168.79.128,只能获取数据节点
         *
         */
        /*ImmutableList<DiscoveryNode> connectedNodes = client.connectedNodes();
        for(DiscoveryNode node : connectedNodes)
        {
            System.out.println(node.getHostAddress());
        }*/
        return client;
    }

    /**
     * 调用 ES 获取 IK 分词后结果
     *
     * @param searchContent
     * @return
     */
    private List<String> getIkAnalyzeSearchTerms(Client client, String searchContent) {
        //调用 IK 分词分词
        AnalyzeRequestBuilder ikRequest = new AnalyzeRequestBuilder(client,
                AnalyzeAction.INSTANCE, index, searchContent);
        //ikRequest.setAnalyzer("ik");
        ikRequest.setTokenizer("ik");
        List<AnalyzeResponse.AnalyzeToken> ikTokenList = ikRequest.execute().actionGet().getTokens();

        //循环赋值
        List<String> searchTermList = new ArrayList<>();
        //ikTokenList.forEach(ikToken -> { searchTermList.add(ikToken.getTerm()); });
        for (AnalyzeResponse.AnalyzeToken ikToken: ikTokenList) {
            System.out.println(ikToken.getTerm());
            searchTermList.add(ikToken.getTerm());
        }

        return searchTermList;
    }
}
