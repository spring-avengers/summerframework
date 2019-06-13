$.fn.serializeObject = function() {
    var o = {};
    var a = this.serializeArray();
    $.each(a, function() {
      if (o[this.name]) {
        if (!o[this.name].push) {
          o[this.name] = [o[this.name]];
        }
        o[this.name].push(this.value || '');
      } else {
        o[this.name] = this.value || '';
      }
    });
    return o;
};

function jump2PreviousPage(targetMenuItemStr,refreshFun){
   var topWindow = $(window.parent.document),
   iframes = topWindow.find('.J_mainContent .J_iframe'),
   tabs = topWindow.find(".J_menuTabs .page-tabs-content .J_menuTab"),
   currentTab2Remove = topWindow.find(".J_menuTabs .page-tabs-content .J_menuTab.active").index();
   //激活目标菜单及页面
   $(window.parent.document).find('#'+targetMenuItemStr)[0].click();
   var nextTab2Show = topWindow.find(".J_menuTabs .page-tabs-content .J_menuTab.active").index();
   refreshFun(iframes.eq(nextTab2Show)[0].contentWindow);
   //移除当前跳转页面
   tabs.eq(currentTab2Remove).remove();
   iframes.eq(currentTab2Remove).remove();
}

function page(url, title) {
    var nav = $(window.parent.document).find('.J_menuTabs .page-tabs-content ');
    $(window.parent.document).find('.J_menuTabs .page-tabs-content ').find(".J_menuTab.active").removeClass("active");
    $(window.parent.document).find('.J_mainContent').find("iframe").css("display", "none");
    var iframe = '<iframe class="J_iframe" name="iframe10000" width="100%" height="100%" src="' + url + '" frameborder="0" data-id="' + url
        + '" seamless="" style="display: inline;"></iframe>';
    $(window.parent.document).find('.J_menuTabs .page-tabs-content ').append(
        ' <a href="javascript:;" class="J_menuTab active" data-id="' + url + '">' + title + ' <i class="fa fa-times-circle"></i></a>');
    $(window.parent.document).find('.J_mainContent').append(iframe);
}

/*关闭iframe*/
function removeIframe() {
    var topWindow = $(window.parent.document),
        iframe = topWindow.find('.J_mainContent .J_iframe'),
        tab = topWindow.find(".J_menuTabs .page-tabs-content .J_menuTab"),
        showTab = topWindow.find(".J_menuTabs .page-tabs-content .J_menuTab.active"),
        i = showTab.index();
        tab.eq(i - 1).addClass("active");
        tab.eq(i).remove();
        iframe.eq(i - 1).show();
        iframe.eq(i).remove();
}

function removeIframeWithSwal(){
    swal({
        title: "关闭确定？",
        text: "请检查本页是否未保存，是否确定关闭？",
        type: "warning",
        showCancelButton: true,
        confirmButtonColor: "#DD6B55",
        confirmButtonText: "关闭",
        closeOnConfirm: false
    }, function () {
        removeIframe();
    });
}

