<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<c:forEach var="answer" items="${requestScope.question.answersDTO.answers}">
    <div class="container-fluid mb-3 mt-3 answer_container">
        <div class="row" id="answer_block">
            <div class="col" id="answer_detail">
                <p class="justify-content-between">
                        ${answer.description}
                </p>
                <div class="answer_footer d-flex flex-row-reverse">
                    <span class="p-2">Creation Date : <fmt:formatDate value="${answer.creationDate}" pattern="dd.MM.yyyy HH:mm"/> </span>
                    <span class="p-2">Author ${answer.username}</span>
                    <btn id='add_comment_answer_toggleVisible' href="javascript:void(0)" class="p-2 btn-link" onclick="toggleVisibility('add-comment-container-${answer.id}' )">Add comment</btn>
                </div>
                <div id="add-comment-container-${answer.id}" class="m-2" style="display: none">
                    <form method="POST" action="${pageContext.request.contextPath}/comment.do">
                        <input type="hidden" id="comment_answer_id" name="comment_answer_id" value="${answer.id.asString()}">
                        <input type="hidden" id="comment_question_id" name="comment_question_id" value="${requestScope.question.id.asString()}">
                        <label for="txt_question_comment">Your comment</label>
                        <textarea class="form-control" id="txt_question_comment" name="txt_question_comment" rows="3" placeholder="Your comment"
                                  required></textarea>
                        <div class="d-flex flex-row-reverse">
                            <button id="bnt_submit_question_comment" name="bnt_submit_question_comment" class="btn btn-primary btn-classic-filled mt-2"
                                    type="submit">Comment this answer
                            </button>
                        </div>
                    </form>
                </div>
                <div id="answer_comment_container">
                    <c:forEach var="comment" items="${answer.comments.comments}">
                        <fmt:formatDate value="${comment.creationDate}" pattern="dd.MM.yyyy HH:mm" var="strDate"/>
                        <jsp:include page="comments.jsp">
                            <jsp:param name="description" value="${comment.description}"/>
                            <jsp:param name="username" value="${comment.username}"/>
                            <jsp:param name="creationDate" value="${strDate}"/>
                        </jsp:include>
                    </c:forEach>
                </div>
            </div>
        </div>
    </div>
</c:forEach>

