$(function() {
  var isStart = false;
  $('#startbtn').click(function() {
    if (!isStart) {
      isStart = true;
      $(this).removeClass('btn-primary');
      $(this).addClass('btn-danger');
      $(this).html('停止搜索');

      var formdata = $('#form').serializeArray();
      formdata = formdata.map((item) => {return item['value'];});
      console.log({"kwords": formdata});
      $.ajax({
        "type": "post",
        "url": "/zawu/start",
        data: {kwords: JSON.stringify(formdata)},
        contentType: "application/x-www-form-urlencoded"
      });
    }
    else {
      isStart = false;
      if (confirm('确定要停止吗？')) {
        $(this).removeClass('btn-danger');
        $(this).addClass('btn-primary');
        $(this).html('开始搜索');

        $.ajax({
          type: "post",
          url: "/zawu/stop"
        });
      }
    }
  });

  $('#loginbtn').click(function() {
    $(this).attr('disabled', true);
    $.post("/zawu/login");
  });

  function freshAppStatus() {
    $.post("/zawu/status", (res) => {
      if (res.length > 0) {
        res.forEach((item) => {
          var platform = item['platform'];
          var status = item['status'];
          var nodeid = "#" + platform + "-status";
          $(nodeid).html(status);
        });
      }
    }, 'json');
  }

  setInterval(freshAppStatus, 5000);

  var page = 1;
  var listdata = [];
  function getResult() {
    $.post('/zawu/result-page', {page: page}, (resp) => {
      if (!resp) {
        return;
      }
      $('.prepage').removeClass('disabled');
      $('.nextpage').removeClass('disabled');
      if (page <= 1) {
        $('.prepage').addClass('disabled');
      }
      if (resp && resp.length < 10) {
        $('.nextpage').addClass('disabled');
      }

      listdata = resp;

      var html = "";
      resp.forEach((item) => {
        html += "<tr>";
        html += "<td>" + item.pagetime + "</td>";
        html += "<td><a href='" + item.url + "' target='_blank'>" + item.title + "</a></td>";
        html += "<td>" + item.platform + "</td>";
        html += "<td>" + item.url + "</td>";
        var ischecked = "";
        if (item.isbad) {
          ischecked = " checked";
        }
        html += "<td>"
          + '<input type="checkbox"' + ischecked +' data-id="' + item.id + '">'
          + "</td>";
        html += "</tr>";
      });
      $('#tbody').html(html);
    }, 'json');
  }

  $('#freshbtn').click(getResult);

  $('table').on('click', 'input', function() {
    var id = $(this).data('id');
    var del = !$(this).prop('checked');
    $.post("/zawu/markbad", {"id": id, "del": del}, function() {

    });
  });

  $('.prepage').click(function() {
    if (page <= 1) { return ;}
    page -= 1;
    getResult();
  });

  $('.nextpage').click(function() {
    if (!listdata || listdata.length < 10) {
      return;
    }
    page += 1;
    getResult();
  });

    $('#downloadbtn').click(function() {
        var $eleForm = $("<form method='post'></form>");
        $eleForm.attr("action","/zawu/download");
        $(document.body).append($eleForm);
        $eleForm.submit();
    });

});
