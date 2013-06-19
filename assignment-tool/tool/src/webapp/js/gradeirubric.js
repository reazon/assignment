//DN 2012-09-24: refresh grade
function refreshGrade(gradebookUId, studentId, gradeObjectId) {
	
	var urlPage = "/sakai-gradebook-tool/iRubricLink.jsp?p=gg&gradebookUid="+ gradebookUId +
		"&siteId="+ gradebookUId +"&rosterStudentId="+ studentId +"&gradebookItemId="+gradeObjectId + 
		"&fieldToUpdate=grade";
	
	//function in file helper.js
	createRubricFrame("getGradeFrame", urlPage);
	$("#irubric-grading-refresh").hide();
}

//DN 2012-09-24: override updateScoreTextbox of helper.js(helper.js in gradebook1)
//because asm1 needs score with 1 decimal
// update score textbox
function updateScoreTextbox(fieldToUpdate, newScore) {
	var inp = document.getElementById(fieldToUpdate);
	if(newScore != '' && newScore > 0) {
		newScore = Math.round(newScore*10)/10;
	}
	inp.value = newScore;
	// remove hidden frame
	var getGradeFrame = document.getElementById("getGradeFrame");
	discardElement(getGradeFrame);
}

//DN 2012-09-24: grade student
function gradeStudent(gradebookUId, studentId, gradeObjectId) {
	
	openWindowiRubric(gradebookUId, studentId, gradeObjectId,"g");
}
