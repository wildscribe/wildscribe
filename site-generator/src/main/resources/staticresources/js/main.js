$(function(){

    $("[data-toggle=popover-provider-points]").popover({
          html : true,
          content: function() {
            return $(this.attributes['data-target'].value).html();
          }
      });

});

