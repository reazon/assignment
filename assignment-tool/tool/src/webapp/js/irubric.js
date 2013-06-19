//DN 2012-09-24: create iRubric link
function getLinkiRubric() {
	var gradebookUId = $('#gradebookUId').val();
	var studentId = $('#studentUId').val();
	var gradeObjectId = $('#gradebookItem').val();
	var purpose =  $('#Purpose').val();
	
	if(gradebookUId != null && gradeObjectId != null){
		$.getJSON("/sakai-gradebook-tool/iRubricLink.jsp?p=ra&tool=asnn2&gradebookUid="+
				gradebookUId+"&siteId="+ gradebookUId+"&gradebookItemId="+gradeObjectId,
	
			function(data){
			    if(data && data[gradeObjectId]) {
			        $("#irubric-area").show();
			        $("#irubric-link").click(function(e) {
			        	
			        	if(purpose == 'grade'){
				            gradeStudent(gradebookUId, studentId,gradeObjectId);
				            $("#irubric-grading-refresh").show();
				            //event refresh grade from irubric
				            $("#irubric-grading-refresh").click(function(e) {
				            	refreshGrade(gradebookUId, studentId,gradeObjectId);
				            });
			        	}else if(purpose == 'view')
			        		viewGrade(gradebookUId, studentId,gradeObjectId);
			            
			        });
			        //refresh grade from irubric
			        $("#irubric-refresh").click(function(e) {
		            	refreshGrade(gradebookUId, studentId,gradeObjectId);
		            });
			    }
			});
	}
}
//DN 2012-09-24: open window irubric
function openWindowiRubric(gradebookUId, studentId, gradeObjectId, purpose) {
	
	var urlPage = "/sakai-gradebook-tool/iRubricLink.jsp?p="+ purpose + "&gradebookUid=" +
				gradebookUId+"&siteId="+ gradebookUId +"&rosterStudentId="+ studentId +
				"&gradebookItemId="+ gradeObjectId;
	
	window.open(urlPage,'_blank',
     'width=800,height=600,top=20,left=100,menubar=yes,status=yes,location=no,toolbar=yes,scrollbars=yes,resizable=yes');
}

//DN 2012-09-24: view grade iRubric
function viewGrade(gradebookUId, studentId, gradeObjectId) {
	
	openWindowiRubric(gradebookUId, studentId, gradeObjectId,"v");
}
