/**
 * Created by liu on 2018/5/24.
 */
var testUrl = 'http://127.0.0.1:8088';
function getOperations() {
    var formData = {};
    var pageIndex = parseInt($.trim($("#pageIndex").val()));
    var pageSize = parseInt($.trim($("#pageSize").val()));
    var startIndex = (pageIndex-1)*pageSize+1;
    var totalRows = parseInt($.trim($("#totalRows").val()));

    if (totalRows == null || totalRows == "") {
        totalRows = 0;
    } else {
        if (Math.ceil(totalRows / pageSize) < pageIndex) {
            alert("每页显示条数与页数之积过大，请重新输入第几页");
            return;
        }
    }

    $("#startIndex").val(startIndex);
    $("#endIndex").val(pageIndex*pageSize);

    formData["keyword"] = $.trim($("#keyword").val());
    formData["pageIndex"] = startIndex;
    formData["pageSize"] = pageSize;
    formData["priceSort"] = $.trim($("#priceSort").val());
    $.ajax({
        url: testUrl + "/searchFromSize.do",
        type : "POST",
        data : formData,
        dataType : "json",
        timeout : 200000,
        success : function(data){

            if (data.success == true) {
                var result = data.result;
                var goods = result.goods;
                if (goods == null || goods.length <= 0) {
                    alert("没有找到符合的商品~")
                }

                $("#totalRows").val(result.totalRows);
                $("#totalPages").val(result.totalRows/pageSize);

                $("#goodsExportList").empty();
                for (var i=0; i<goods.length; i++) {
                    var s = goods[i];
                    var imageTemp = "  ";
                    if (s.image.length > 4) {
                        imageTemp = s.image.substring(0, 4) + "···";
                    } else {
                        imageTemp = s.image;
                    }
                    $("#goodsExportList").append(
                        '<tr>' +
                        '<td>' + s.skuId + '</td>' +
                        '<td>' + s.name + '</td>' +
                        '<td>' + s.price + '</td>' +
                        '<td>' + imageTemp + '</td>' +
                            '</tr>'
                    )
                }
            }

        },
        error: function (XMLHttpRequest, textStatus, errorThrown) {
            alert("获取商品详情失败！");
            console.log(XMLHttpRequest.status);
            console.log(XMLHttpRequest.readyState);
            console.log(textStatus);
        }
    });
}

function prePage() {
    var pageIndex = parseInt($.trim($("#pageIndex").val()));
    var pageSize = parseInt($.trim($("#pageSize").val()));
    if (pageIndex >= 2) {
        $("#pageIndex").val(pageIndex - 1);
        $("#startIndex").val((pageIndex-2)*pageSize+1);
        $("#endIndex").val((pageIndex-1)*pageSize);
        getOperations();
    } else {
        $("#pageIndex").val(1);
        alert("已到达首页");
    }
}

function nextPage() {
    var pageIndex = parseInt($.trim($("#pageIndex").val()));
    var pageSize = parseInt($.trim($("#pageSize").val()));
    var totalRows = parseInt($.trim($("#totalRows").val()));
    var totalPages = Math.ceil(totalRows/pageSize);
    if (pageIndex >= totalPages) {
        $("#pageIndex").val(totalPages);
        alert("已到达最后一页");
    } else {
        $("#pageIndex").val(pageIndex + 1);
        $("#startIndex").val(pageIndex*pageSize+1);
        $("#endIndex").val((pageIndex+1)*pageSize);
        getOperations();
    }
}

function getOperationsByScroll() {
    var formData = {};
    var pageIndex = parseInt($.trim($("#pageIndexScroll").val()));
    var pageSize = parseInt($.trim($("#pageSizeScroll").val()));
    var startIndex = (pageIndex-1)*pageSize+1;
    var totalRows = parseInt($.trim($("#totalRowsScroll").val()));
    var scrollId = $.trim($("#scrollId").val());

    if (totalRows == null || totalRows == "") {
        totalRows = 0;
    } else {
        if (Math.ceil(totalRows / pageSize) < pageIndex) {
            alert("每页显示条数与页数之积过大，请重新输入第几页");
            return;
        }
    }

    $("#startIndexScroll").val(startIndex);
    $("#endIndexScroll").val(pageIndex*pageSize);

    formData["keyword"] = $.trim($("#keywordScroll").val());
    formData["scrollId"] = scrollId;
    formData["pageSize"] = pageSize;
    formData["priceSort"] = $.trim($("#priceSortScroll").val());
    $.ajax({
        url: testUrl + "/searchScroll.do",
        type : "POST",
        data : formData,
        dataType : "json",
        timeout : 200000,
        success : function(data){

            if (data.success == true) {
                var result = data.result;
                var goods = result.goods;
                if (goods == null || goods.length <= 0) {
                    alert("没有找到符合的商品~")
                }

                $("#totalRowsScroll").val(result.totalRows);
                $("#totalPagesScroll").val(result.totalRows/pageSize);
                $("#scrollId").val(result.scrollId);

                $("#goodsExportListScroll").empty();
                for (var i=0; i<goods.length; i++) {
                    var s = goods[i];
                    var imageTemp = "  ";
                    if (s.image.length > 4) {
                        imageTemp = s.image.substring(0, 4) + "···";
                    } else {
                        imageTemp = s.image;
                    }
                    $("#goodsExportListScroll").append(
                        '<tr>' +
                        '<td>' + s.skuId + '</td>' +
                        '<td>' + s.name + '</td>' +
                        '<td>' + s.price + '</td>' +
                        '<td>' + imageTemp + '</td>' +
                        '</tr>'
                    )
                }
            }

        },
        error: function (XMLHttpRequest, textStatus, errorThrown) {
            alert("获取商品详情失败！");
            console.log(XMLHttpRequest.status);
            console.log(XMLHttpRequest.readyState);
            console.log(textStatus);
        }
    });
}

function prePageScroll() {
    var pageIndex = parseInt($.trim($("#pageIndexScroll").val()));
    var pageSize = parseInt($.trim($("#pageSizeScroll").val()));
    if (pageIndex >= 2) {
        $("#pageIndexScroll").val(pageIndex - 1);
        $("#startIndexScroll").val((pageIndex-2)*pageSize+1);
        $("#endIndexScroll").val((pageIndex-1)*pageSize);
        getOperationsByScroll();
    } else {
        $("#pageIndexScroll").val(1);
        alert("已到达首页");
    }
}

function nextPageScroll() {
    var pageIndex = parseInt($.trim($("#pageIndexScroll").val()));
    var pageSize = parseInt($.trim($("#pageSizeScroll").val()));
    var totalRows = parseInt($.trim($("#totalRowsScroll").val()));
    var totalPages = Math.ceil(totalRows/pageSize);
    if (pageIndex >= totalPages) {
        $("#pageIndexScroll").val(totalPages);
        alert("已到达最后一页");
    } else {
        $("#pageIndexScroll").val(pageIndex + 1);
        $("#startIndexScroll").val(pageIndex*pageSize+1);
        $("#endIndexScroll").val((pageIndex+1)*pageSize);
        getOperationsByScroll();
    }
}