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

    var page = 1;
    var listdata = [];
    $('#freshbtn').click(function() {
        $.post('/zawu/result-page', {page: page}, (resp) => {
            listdata = resp;
            console.log(resp);
        }, 'json');
    });


});
