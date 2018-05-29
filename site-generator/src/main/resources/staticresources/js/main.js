$(function(){

    $("[data-toggle=popover-provider-points]")
      .on('click', function(e){
            e.preventDefault();
       })
       .popover({
          html : true,
          content: function() {
            return $(this.attributes['data-target'].value).html();
          }
      });

});

