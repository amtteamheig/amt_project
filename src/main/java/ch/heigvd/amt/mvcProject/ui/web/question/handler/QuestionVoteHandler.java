package ch.heigvd.amt.mvcProject.ui.web.question.handler;

import ch.heigvd.amt.mvcProject.application.ServiceRegistry;
import ch.heigvd.amt.mvcProject.application.authentication.CurrentUserDTO;
import ch.heigvd.amt.mvcProject.application.question.QuestionFacade;
import ch.heigvd.amt.mvcProject.application.question.QuestionFailedException;
import ch.heigvd.amt.mvcProject.application.user.exceptions.UserFailedException;
import ch.heigvd.amt.mvcProject.domain.question.QuestionId;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static ch.heigvd.amt.mvcProject.application.VoteUtils.DOWNVOTE;
import static ch.heigvd.amt.mvcProject.application.VoteUtils.UPVOTE;

@WebServlet(name = "QuestionVoteHandler", urlPatterns = "/q_vote")
public class QuestionVoteHandler extends HttpServlet {

    @Inject
    private ServiceRegistry serviceRegistry;
    private QuestionFacade questionFacade;

    @Override
    public void init() throws ServletException {
        super.init();
        questionFacade = serviceRegistry.getQuestionFacade();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        CurrentUserDTO currentUser = (CurrentUserDTO) req.getSession().getAttribute("currentUser");
        QuestionId questionId = new QuestionId(req.getParameter("id"));

        String vote = req.getParameter("vote");
        try {

            if(vote.equals("upvote")) {
                questionFacade.vote(currentUser.getUserId(), questionId, UPVOTE);
            } else if (vote.equals("downvote")){
                questionFacade.vote(currentUser.getUserId(), questionId, DOWNVOTE);
            }

            // refresh the page
            resp.sendRedirect("/question?id=" + questionId.asString());
        } catch (QuestionFailedException | UserFailedException e) {
            e.printStackTrace();
        }
    }
}
