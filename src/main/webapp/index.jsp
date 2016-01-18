<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
 <head>
<!--   <script type="text/javascript" src="jquery-1.7.2.min.js"></script>  -->
      <script language="JavaScript" type="text/javascript" src="jquery-latest.js"> </script>
   <link rel="stylesheet" type="text/css" href="<%=request.getContextPath()%>/upload/uploadify.css">  
     <script type="text/javascript" src="<%=request.getContextPath()%>/upload/jquery.uploadify.min.js"></script> 
  <script >
  	var fileName = '';
      $(document).ready(function() {  
            $("#file_upload").uploadify({  
                    'buttonText' : 'select an individual diagnostic criterion',  
                    'height' : 30,  
                    'swf' : '<%=request.getContextPath()%>/upload/uploadify.swf',  
                    'uploader' : 'uploadFile.do',  
                    'width' : 240,  
                    'auto':false,  
                    'fileObjName'   : 'file',  
                    'onUploadSuccess' : function(file, data, response) {  
                        alert( file.name + ' upload successed！ ');  
                        $("#textarea").val(data);
                    },
                    'onSelect' : function(file) {                             
                    	fileName = file.name;
                    }
                });  
        });  
      $(function(){
    	  $("#btn").click(function(){
    		  var textContent = $("textarea").val();
    		  if(textContent.length ==0){
    			  alert("input empty!");
    		  }
    		  var url = "parse.do";
    		  $.ajax({
	  				url:url,
	  				async:false,
	  				type:"post",
	  				data:{"textContent":textContent},
	  				success:function(data){
	  					alert(data);
	  					$("#xmltextarea").val(data);
	  				}
	  			});
    	  });
      });
  	function upload(){
  		$('#file_upload').uploadify('upload', '*');
  	}
  </script>
 </head>
 <body>
<!--   <form method="post" action="upload.do" enctype="multipart/form-data"> -->
<!--    <input type="file" name="file" />xml -->
<!--    <input type="submit" name="上传" value="上传"/> -->
<!--   </form> -->
  <div><span style="font-size:18px;color:#252525;"> Input an individual diagnostic criterion</span><br>
  <span> OR </span>
  <input type="file" name="fileName" id="file_upload" ></input></div>
  <a href="javascript:upload();">upload file</a> | <a href="javascript:$('#file_upload').uploadify('stop')">stop upload!</a>  
  <div>
  <div>
  <textarea cols="100" rows="20" id="textarea" name="textarea" style="color: black;overflow-x:auto;">
  </textarea>
  <br>
  <button id="btn" name="btn">submit</button>
  <hr>
  <span> QDM\HQMF XML Output:</span>
  <br>
   <textarea cols="100" rows="20" id="xmltextarea" name="xmltextarea" style="color: blue;overflow-x:auto;" readonly>
  </textarea>
  </div>
  </div>
 </body>
</html>