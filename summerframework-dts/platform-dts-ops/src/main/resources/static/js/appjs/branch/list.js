$(function () {
    $("#stateSelect").change(function () {
        reLoad();
    })
    load();
});

function load() {
    $('#branchTable').bootstrapTable({
        method: 'get',
        url: "/branch" ,
        striped: true,
        dataType: "json",
        pagination: true,
        singleSelect: false,
        pageSize: 10,
        pageList: [5, 10, 20],
        pageNumber: 1,
        sidePagination: "server",
        showRefresh: true,
        iconSize: "outline",
        icons: {refresh: "glyphicon-repeat"},
        toolbar: "#branchToolbar",
        queryParams: function (params) {
            return {
                limit: params.limit,
                offset: params.offset,
                transId:$("#transId").val(),
                state:$("#stateSelect").val()
            };
        },
        columns: [{
            checkbox: true
        }, {
              field: 'transId',
              title: '事务编号'
        }, {
            field: 'branchId',
            title: '分支编号'
        }, {
            field: 'resourceIp',
            title: '资源Ip'
        }, {
            field: 'resourceInfo',
            title: '资源信息'
        }, {
          field: 'stateStr',
          title: '状态'
        }, {
          field: 'createdTimeStr',
          title: '创建时间'
        }, {
          field: 'modifiedTimeStr',
          title: '更新时间',
        }, {
            title: '错误详情',
            field: 'id',
            width: '70px',
            align: 'center',
            formatter: function (value, row, index) {
                var e ;
                if(row.state == 3) {
                    e = '<a href="javascript:void(0)" mce_href="#" title="异常信息" onclick="viewError(\'' + row.branchId + '\')">' +
                        '<i class="glyphicon glyphicon-info-sign"></i></a> ';
                }
                return e;
            }
        }]
    });

}

function reLoad() {
    $('#branchTable').bootstrapTable('refresh');
}

function viewError(branchId) {
    $.ajax({
        type: "GET",
        url: "/branch/error/"+branchId,
        contentType: "application/json; charset=utf-8",
        dataType: "json",
        success: function(data) {
            swal("分支 ["+data.branchId+"] 错误详情！", "详情：" + data.resourceInfo + "\n 是否通知：" + data.notified);
        },
        error: function(err) {
            alert(err);
        }
    });
}
