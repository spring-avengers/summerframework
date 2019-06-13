$(function () {
    $("#startTime").datetimepicker();
    $("#endTime").datetimepicker();
    $("#stateSelect").change(function () {
        reLoad();
    })
    $("#endTime").change(function () {
        reLoad();
    })
    $("#startTime").change(function () {
        if($("#endTime").val()){
            reLoad();
        }
    })
    load();
});

function load() {
    $('#dtsTable').bootstrapTable({
        method: 'get',
        url: "/globalRecord" ,
        striped: true,
        dataType: "json",
        pagination: true,
        singleSelect: false,
        pageSize: 5,
        pageList: [5, 10, 20],
        pageNumber: 1,
        sidePagination: "server",
        showRefresh: true,
        iconSize: "outline",
        icons: {refresh: "glyphicon-repeat"},
        toolbar: "#dtsToolbar",
        queryParams: function (params) {
            return {
                limit: params.limit,
                offset: params.offset,
                state:$("#stateSelect").val(),
                startTime:$("#startTime").val(),
                endTime:$("#endTime").val(),
            };
        },
        columns: [{
            checkbox: true
        }, {
              field: 'transId',
              title: '事务编号'
        }, {
          field: 'stateStr',
          title: '状态'
        },{
            field: 'clientIp',
            title: '发起事务者IP'
        },{
            field: 'clientInfo',
            title: '发起事务者的信息'
        }, {
          field: 'createdTimeStr',
          title: '创建时间'
        }, {
          field: 'modifiedTimeStr',
          title: '更新时间',
        }, {
            title: '分支详情',
            field: 'id',
            width: '70px',
            align: 'center',
            formatter: function (value, row, index) {
                var e = '<a href="javascript:void(0)" mce_href="#" title="分支详情" onclick="edit(\'' + row.transId + '\')">' +
                    '<i class="glyphicon glyphicon-edit"></i></a> ';
                return e;
            }
        }]
    });

}

function reLoad() {
    $('#dtsTable').bootstrapTable('refresh');
}

function edit(id) {
    var title = '分支详情';
    page("/branch/list/"+id, title);
    //loadURL("/org/edit/" + id, $('#content-main'));
}
