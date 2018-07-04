import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.action.admin.indices.analyze.AnalyzeAction;
import org.elasticsearch.action.admin.indices.analyze.AnalyzeRequestBuilder;
import org.elasticsearch.action.admin.indices.analyze.AnalyzeResponse;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.Requests;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHits;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

/**
 * 类功能描述:Elasticsearch的基本测试
 *
 * @author liu
 * @since 2018/5/24
 */
public class ElasticsearchTest {

    private Logger logger = LoggerFactory.getLogger(ElasticsearchTest.class);

    public final static String HOST = "10.10.13.234";

    /** http请求的端口是9200，客户端是9300 */
    public final static int PORT = 9300;

    Client client;
    /** 索引库名 */
    private static String index = "my_index";
    /** 类型名称 */
    private static String type = "skuId";
    private static String clusterName = "my-application";

    /**
     * 创建mapping，注意：每次只能创建一次
     * 创建mapping(field("indexAnalyzer","ik")该字段分词IK索引 ；field("searchAnalyzer","ik")该字段分词ik查询；具体分词插件请看IK分词插件说明)
     * 创建mapping(field("analyzer","ik")该字段分词IK索引 ；field("index","analyzed")该字段分词ik查询；具体分词插件请看IK分词插件说明)2.x版
     * @param indices 索引名称；
     * @param mappingType 类型
     * @throws Exception
     */
    @Test
    public void createMapping()throws Exception {
        String indices = "jd_mall_v2";
        String mappingType = "goods";

        Settings settings = Settings.settingsBuilder()
                .put("cluster.name", clusterName)
                .put("client.transport.sniff", true)
                .build();

        Client client = TransportClient.builder().settings(settings).build()
                .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(HOST), PORT));

        client.admin().indices().prepareCreate(indices).execute().actionGet();
        new XContentFactory();
        XContentBuilder builder=XContentFactory.jsonBuilder()
                .startObject()
                .startObject(mappingType)
                .startObject("properties")
                .startObject("name").field("type", "string").field("store", "yes").field("analyzer","ik").field("index","analyzed").endObject()
                .startObject("price").field("type", "long").endObject()
                .endObject()
                .endObject()
                .endObject();
        PutMappingRequest mapping = Requests.putMappingRequest(indices).type(mappingType).source(builder);

        client.admin().indices().putMapping(mapping).actionGet();
        client.close();
    }



    @Before
    public void before()
    {

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

    }

    /**
     * 测试Elasticsearch客户端连接
     * @Title: test1
     * @author sunt
     * @date 2017年11月22日
     * @return void
     * @throws UnknownHostException
     */
    @SuppressWarnings("resource")
    @Test
    public void test1() throws UnknownHostException {
        //创建客户端
        /*TransportClient client = new PreBuiltTransportClient(Settings.EMPTY).addTransportAddresses(
                new InetSocketTransportAddress(InetAddress.getByName(HOST),PORT));*/
        TransportClient client = TransportClient.builder().build().addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(HOST), PORT));

        logger.debug("Elasticsearch connect info:" + client.toString());
        System.out.println("Elasticsearch connect info:" + client.toString());

        //关闭客户端
        client.close();
    }

    /**
     * 查
     *
     * @throws Exception
     */
    @Test
    public void testGet() throws Exception {

        String skuId = "3724941";
        String skuName = "华盛顿苹果礼盒 8个装（4个红蛇果+4个青苹果）单果重约145g-180g 新鲜水果礼盒";
        String skuPrice = "4990";

        //获取_id为1的类型
        /*GetResponse response1 = client.prepareGet(index, "person", "9").get();
        response1.getSourceAsMap();//获取值转换成map
        System.out.println("查询一条数据:" + JSON.toJSON(response1.getSourceAsMap()));*/

        //获取分词
        List<String> listAnalysis = getIkAnalyzeSearchTerms("进口水果");

        //List<Map<String, Object>> resultList6 = testMultiQueryStringQuery(index, type, "name", listAnalysis);
        List<Map<String, Object>> resultList3 = testQueryStringQuery(index, type, "name", "进口水果");
        //List<Map<String, Object>> resultList4 = getDataByWildcard(index, type, "name", "*水果*");
        //List<Map<String, Object>> resultList5 = getDataByMatch(index, type, "name", "进口 水果");
        //List<Map<String, Object>> resultList = getQueryDataBySingleField(index, type, "name", "进口水果");
        //List<Map<String, Object>> resultList2 = testSearchByPrefix(index, "name", "进口水果");
        //System.out.println();


    }

    /**
     * 增查
     *
     * @throws Exception
     */
    @Test
    public void testCreate() throws Exception {
        String[] skuIds = {"2246904", "2138027", "3724941", "5664705", "3711635"};
        String[] skuNames = {"果花 珍珠岩 无土栽培基质 颗粒状 保温性能好 园艺用品",
                            "进口华盛顿红蛇果 苹果4个装 单果重约180g 新鲜水果",
                            "华盛顿苹果礼盒 8个装（4个红蛇果+4个青苹果）单果重约145g-180g 新鲜水果礼盒",
                            "新疆阿克苏冰糖心 约5kg 单果200-250g（7Fresh 专供）",
                            "江西名牌 杨氏精品脐橙 5kg装 铂金果 水果橙子礼盒 2种包装随机发货"};
        String[] skuPrices = {"500", "3800", "4990", "8500", "5880"};

        for (int i=0; i<skuIds.length; i++) {
            String skuId = skuIds[i];
            String skuName = skuNames[i];
            String skuPrice = skuPrices[i];

            String jsonStr = "{" +
                    "\"skuId\":\""+skuId+"\"," +
                    "\"skuName\":\""+skuName+"\"," +
                    "\"skuPrice\":\""+skuPrice+"\"" +
                    "}";


            //参数说明： 索引，类型 ，_id；//setSource可以传以上map string  byte[] 几种方式
            IndexResponse response = client.prepareIndex(index, type, skuId)
                    .setSource(jsonStr,XContentType.JSON)
                    .get();
            boolean created = response.isCreated();
            System.out.println("创建一条记录:" + created);

            //获取_id为1的类型
            GetResponse response1 = client.prepareGet(index, type, skuId).get();
            response1.getSourceAsMap();//获取值转换成map
            System.out.println("查询一条数据:" + JSON.toJSON(response1.getSourceAsMap()));
        }
    }

    /**
     * 删查
     *
     * @throws Exception
     */
    @Test
    public void testDelete() throws Exception {
        String skuId = "3724941";
        String skuName = "华盛顿苹果礼盒 8个装（4个红蛇果+4个青苹果）单果重约145g-180g 新鲜水果礼盒";
        String skuPrice = "4990";
        String skuImage = "jfs/t16450/249/461724533/219792/7f204d7a/5a321ecbN4526f7d3.jpg";

        //删除_id为1的类型
        DeleteResponse response2 = client.prepareDelete(index, type, skuId).get();
        System.out.println("删除一条数据：" + response2.isFound());

        //获取_id为1的类型
        GetResponse response1 = client.prepareGet(index, type, skuId).get();
        response1.getSourceAsMap();//获取值转换成map
        System.out.println("查询一条数据:" + JSON.toJSON(response1.getSourceAsMap()));
    }

    /**
     * 改查
     *
     * @throws Exception
     */
    @Test
    public void testUpdate() throws Exception {
        String skuId = "3724941";
        String skuName = "华盛顿苹果礼盒 8个装（4个红蛇果+4个青苹果）单果重约145g-180g 新鲜水果礼盒";
        String skuPrice = "4990";
        String skuName2 = "华圣 陕西精品红富士苹果 12个装 果径75mm 约2.1kg 新鲜水果";
        //更新
        UpdateResponse updateResponse = client.prepareUpdate(index, type, skuId).setDoc(jsonBuilder()
                .startObject()
                .field("name", skuName2)
                .endObject())
                .get();
        System.out.println("更新一条数据:" + updateResponse.isCreated());

        //获取_id为1的类型
        GetResponse response1 = client.prepareGet(index, type, skuId).get();
        response1.getSourceAsMap();//获取值转换成map
        System.out.println("查询一条数据:" + JSON.toJSON(response1.getSourceAsMap()));
    }

    /**
     * 将对象通过jackson.databind转换成byte[]
     * 注意一下date类型需要格式化处理  默认是 时间戳
     *
     * @return
     */
    public byte[] convertByteArray(Object obj) {
        // create once, reuse
        ObjectMapper mapper = new ObjectMapper();
        try {
            byte[] json = mapper.writeValueAsBytes(obj);
            return json;
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 将对象通过JSONtoString转换成JSON字符串
     * 使用fastjson 格式化注解  在属性上加入 @JSONField(format="yyyy-MM-dd HH:mm:ss")
     *
     * @return
     */
    public String jsonStr(Object obj) {
        return JSON.toJSONString(obj);
    }

    /*****************************/
    /**
            * 根据文档名、字段名、字段值查询某一条记录的详细信息；query查询
     * @param type  文档名，相当于oracle中的表名，例如：ql_xz；
            * @param key 字段名，例如：bdcqzh
     * @param value  字段值，如：“”
            * @return  List
     * @author Lixin
     */
    public List getQueryDataBySingleField(String index, String type, String key, String value){
        //TransportClient client = getClient();
        QueryBuilder qb = QueryBuilders.termQuery(key, value);
        SearchResponse response = client.prepareSearch(index).setTypes(type)
                .setQuery(qb)
                .setFrom(0).setSize(10000).setExplain(true)
                .execute()
                .actionGet();
        return responseToList(client,response);
    }


    /**
     * 多条件  文档名、字段名、字段值，查询某一条记录的详细信息
     * @param type 文档名，相当于oracle中的表名，例如：ql_xz
     * @param map 字段名：字段值 的map
     * @return List
     * @author Lixin
     */
    public List getBoolDataByMuchField(String index, String type, Map<String,String> map){
        //TransportClient client = getClient();
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        for (String in : map.keySet()) {
            //map.keySet()返回的是所有key的值
            String str = map.get(in);//得到每个key多对用value的值
            boolQueryBuilder.must(QueryBuilders.termQuery(in,str));
        }
        SearchResponse response = client.prepareSearch(index).setTypes(type)
                .setQuery(boolQueryBuilder)
                .setFrom(0).setSize(10000).setExplain(true)
                .execute()
                .actionGet();
        return responseToList(client,response);
    }


    /**
     * 单条件 通配符查询
     * @param type 文档名，相当于oracle中的表名，例如：ql_xz
     * @param key 字段名，例如：bdcqzh
     * @param value 字段名模糊值：如 *123* ;?123*;?123?;*123?;
     * @return List
     * @author Lixin
     */
    public List getDataByWildcard(String index, String type, String key, String value){
        //TransportClient client = getClient();
        WildcardQueryBuilder wBuilder = QueryBuilders.wildcardQuery(key, value);
        SearchResponse response = client.prepareSearch(index).setTypes(type)
                .setQuery(wBuilder)
                .setFrom(0).setSize(10000).setExplain(true)
                .execute()
                .actionGet();
        return responseToList(client,response);
    }

    /**
     * 多条件 通配符查询
     * @param type  type 文档名，相当于oracle中的表名，例如：ql_xz
     * @param map   包含key:value 模糊值键值对
     * @return List
     * @author Lixin
     */
    public List getDataByMuchWildcard(String index, String type, Map<String,String> map){
        //TransportClient client = getClient();
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        for (String in : map.keySet()) {
            //map.keySet()返回的是所有key的值
            String str = map.get(in);//得到每个key多对用value的值
            boolQueryBuilder.must(QueryBuilders.wildcardQuery(in,str));
        }
        SearchResponse response = client.prepareSearch(index).setTypes(type)
                .setQuery(boolQueryBuilder)
                .setFrom(0).setSize(10000).setExplain(true)
                .execute()
                .actionGet();

        return responseToList(client,response);
    }

    /**
     * 单条件 模糊拆分查询
     * @param type 文档名，相当于oracle中的表名，例如：ql_xz
     * @param key 字段名，例如：bdcqzh
     * @param value 字段名模糊值：如 *123* ;?123*;?123?;*123?;
     * @return List
     * @author Lixin
     */
    public List getDataByMatch(String index, String type, String key, String value){
        //TransportClient client = getClient();
        MatchQueryBuilder mBuilder = QueryBuilders.matchQuery(key, value);
        SearchResponse response = client.prepareSearch(index).setTypes(type)
                .setQuery(mBuilder)
                .setFrom(0).setSize(10000).setExplain(true)
                .execute()
                .actionGet();
        return responseToList(client,response);
    }
    /**
     * 单条件 中文分词模糊查询
     * @param type 文档名，相当于oracle中的表名，例如：ql_xz
     * @param key 字段名，例如：bdcqzh
     * @param value 字段名模糊值：如 *123* ;?123*;?123?;*123?;
     * @return List
     * @author Lixin
     */
    public List getDataByPrefix(String index, String type, String key, String value){
        //TransportClient client = getClient();
        PrefixQueryBuilder pBuilder = QueryBuilders.prefixQuery(key, value);
        SearchResponse response = client.prepareSearch(index).setTypes(type)
                .setQuery(pBuilder)
                .setFrom(0).setSize(10000).setExplain(true)
                .execute()
                .actionGet();
        return responseToList(client,response);
    }

    /**
     * 中文分词的操作
     * 1.查询以"中"开头的数据，有两条
     * 2.查询以“中国”开头的数据，有0条
     * 3.查询包含“烂”的数据，有1条
     * 4.查询包含“烂摊子”的数据，有0条
     * 分词：
     *      为什么我们搜索China is the greatest country~
     *                 中文：中国最牛逼
     *
     *                 ×××
     *                      中华
     *                      人民
     *                      共和国
     *                      中华人民
     *                      人民共和国
     *                      华人
     *                      共和
     *      特殊的中文分词法：
     *          庖丁解牛
     *          IK分词法
     *          搜狗分词法
     */
    public List testSearchByPrefix(String index, String key, String value) {
        // 在prepareSearch()的参数为索引库列表，意为要从哪些索引库中进行查询
        SearchResponse response = client.prepareSearch(index)
                .setSearchType(SearchType.DEFAULT)  // 设置查询类型，有QUERY_AND_FETCH  QUERY_THEN_FETCH  DFS_QUERY_AND_FETCH  DFS_QUERY_THEN_FETCH
                //.setSearchType(SearchType.DEFAULT)  // 设置查询类型，有QUERY_AND_FETCH  QUERY_THEN_FETCH  DFS_QUERY_AND_FETCH  DFS_QUERY_THEN_FETCH
                //.setQuery(QueryBuilders.prefixQuery("content", "烂摊子"))// 设置相应的query，用于检索，termQuery的参数说明：name是doc中的具体的field，value就是要找的具体的值
//                .setQuery(QueryBuilders.regexpQuery("content", ".*烂摊子.*"))
                .setQuery(QueryBuilders.prefixQuery(key, value))
                .get();
        return responseToList(client,response);
    }

    public List testFuzzyQuery(String index, String type, String key, String value){
        //TransportClient client = getClient();
        FuzzyQueryBuilder fBuilder = QueryBuilders.fuzzyQuery(key, value);
        SearchResponse response = client.prepareSearch(index).setTypes(type)
                .setQuery(fBuilder)
                .setFrom(0).setSize(10000).setExplain(true)
                .execute()
                .actionGet();
        return responseToList(client,response);
    }
    public List testQueryStringQuery(String index, String type, String key, String value){
        //TransportClient client = getClient();
        QueryBuilder fBuilder = QueryBuilders.queryStringQuery(value).field(key);
        SearchResponse response = client.prepareSearch(index).setTypes(type)
                .setQuery(fBuilder)
                .setFrom(0).setSize(10000).setExplain(true)
                .execute()
                .actionGet();
        return responseToList(client,response);
    }
    public List testMultiQueryStringQuery(String index, String type, String key, List<String> listAnalysis){
        //TransportClient client = getClient();
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        for (String term : listAnalysis) {
            boolQueryBuilder.should(QueryBuilders.queryStringQuery(term).field(key));
            //这里可以用must 或者 should 视情况而定
        }
        SearchResponse response = client.prepareSearch(index).setTypes(type)
                .setQuery(boolQueryBuilder)
                .setFrom(0).setSize(10000).setExplain(true)
                .execute()
                .actionGet();
        return responseToList(client,response);
    }


    /**
     * 将查询后获得的response转成list
     * @param client
     * @param response
     * @return
     */
    public List responseToList(Client client, SearchResponse response){
        SearchHits hits = response.getHits();
        List<Map<String, Object>> list = new ArrayList<Map<String,Object>>();
        System.out.println("------------------------");
        for (int i = 0; i < hits.getHits().length; i++) {
            Map<String, Object> map = hits.getAt(i).getSource();
            System.out.println(map.get("name"));
            list.add(map);
        }
        System.out.println("------------------------");
        //client.close();
        return list;
    }



    /**
     * 调用 ES 获取 IK 分词后结果
     *
     * @param searchContent
     * @return
     */
    private List<String> getIkAnalyzeSearchTerms(String searchContent) {
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

    /**
     * 然后就是创建两个查询过程了 ，下面是from-size分页的执行代码：
     */
    @Test
    public void testFromSize() {
        System.out.println("from size 模式启动！");
        long begin = System.currentTimeMillis();
        long count = client.prepareCount(index).setTypes(type).execute()
                .actionGet().getCount();
        //QueryBuilder qBuilder = QueryBuilders.queryStringQuery(value).field(key);
        SearchRequestBuilder requestBuilder = client.prepareSearch(index).setTypes(type)
                .setQuery(QueryBuilders.queryStringQuery("进口水果").field("name"));
        for (int i=0,sum=0; sum<count; i++) {
            SearchResponse response = requestBuilder.setFrom(i).setSize(3).execute().actionGet();
            sum += response.getHits().hits().length;
            responseToList(client,response);
            System.out.println("总量"+count+", 已经查到"+sum);
        }
        System.out.println("elapsed<"+(System.currentTimeMillis()-begin)+">ms.");
    }
    /**
     * 下面是scroll分页的执行代码，注意啊！scroll里面的size是相对于每个分片来说的，
     * 所以实际返回的数量是：分片的数量*size
     */
    @Test
    public void testScroll() {
        //String index, String type,String key,String value
        System.out.println("scroll 模式启动！");
        long begin = System.currentTimeMillis();
        //QueryBuilder qBuilder = QueryBuilders.queryStringQuery(value).field(key);
        //获取Client对象,设置索引名称,搜索类型(SearchType.SCAN),搜索数量,发送请求
        SearchResponse scrollResponse = client.prepareSearch(index)
                .setTypes(type)
                .setQuery(QueryBuilders.queryStringQuery("进口水果").field("name"))
                .setSearchType(SearchType.SCAN).setSize(3).setScroll(TimeValue.timeValueMinutes(1))
                .execute().actionGet();
        //注意:首次搜索并不包含数据，第一次不返回数据，获取总数量
        long count = scrollResponse.getHits().getTotalHits();
        for (int i=0,sum=0; sum<count; i++) {
            scrollResponse = client.prepareSearchScroll(scrollResponse.getScrollId())
                    .setScroll(TimeValue.timeValueMinutes(8)).execute().actionGet();
            sum += scrollResponse.getHits().hits().length;
            responseToList(client,scrollResponse);
            System.out.println("总量"+count+", 已经查到"+sum);
        }
        System.out.println("elapsed<"+(System.currentTimeMillis()-begin)+">ms.");
    }
}
