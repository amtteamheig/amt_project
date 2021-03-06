package ch.heigvd.amt.mvcProject.application.question;

import ch.heigvd.amt.mvcProject.APIUtils;
import ch.heigvd.amt.mvcProject.ApiFailException;
import ch.heigvd.amt.mvcProject.application.answer.AnswerFailedException;
import ch.heigvd.amt.mvcProject.application.answer.AnswersDTO;
import ch.heigvd.amt.mvcProject.application.comment.CommentFacade;
import ch.heigvd.amt.mvcProject.application.comment.CommentFailedException;
import ch.heigvd.amt.mvcProject.application.comment.CommentQuery;
import ch.heigvd.amt.mvcProject.application.comment.CommentsDTO;
import ch.heigvd.amt.mvcProject.application.user.UserFacade;
import ch.heigvd.amt.mvcProject.application.user.UserQuery;
import ch.heigvd.amt.mvcProject.application.user.UsersDTO;
import ch.heigvd.amt.mvcProject.application.user.exceptions.UserFailedException;
import ch.heigvd.amt.mvcProject.domain.question.IQuestionRepository;
import ch.heigvd.amt.mvcProject.domain.question.Question;
import ch.heigvd.amt.mvcProject.domain.question.QuestionId;
import ch.heigvd.amt.mvcProject.domain.user.UserId;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static ch.heigvd.amt.mvcProject.application.VoteUtils.*;

/**
 * Link the question and the domain, what we offer to the user to interact with the domain
 * In this class we pass a command (to modify data) of a query (to get data)
 */
public class QuestionFacade {

    private IQuestionRepository questionRepository;

    private UserFacade userFacade;

    private CommentFacade commentFacade;

    private APIUtils apiUtils;

    public QuestionFacade() {
    }

    public QuestionFacade(IQuestionRepository questionRepository, UserFacade userFacade, CommentFacade commentFacade, APIUtils utils) {
        this.questionRepository = questionRepository;
        this.userFacade = userFacade;
        this.commentFacade = commentFacade;
        this.apiUtils = utils;
    }

    public QuestionsDTO.QuestionDTO addQuestion(QuestionCommand command)
            throws UserFailedException, QuestionFailedException {
        UsersDTO existingUser = userFacade.getUsers(UserQuery.builder().userId(command.getUserId()).build());

        if (existingUser.getUsers().size() == 0)
            throw new QuestionFailedException("The user hasn't been found");

        try {

            UsersDTO.UserDTO user = existingUser.getUsers().get(0);

            Question submittedQuestion = Question.builder()
                    .title(command.getTitle())
                    .description(command.getDescription())
                    .userId(user.getId())
                    .username(user.getUsername())
                    .creationDate(command.getCreationDate())
                    .build();

            questionRepository.save(submittedQuestion);

            QuestionsDTO.QuestionDTO newQuestion = QuestionsDTO.QuestionDTO.builder()
                    .description(submittedQuestion.getDescription())
                    .id(submittedQuestion.getId())
                    .title(submittedQuestion.getTitle())
                    .username(submittedQuestion.getUsername())
                    .creationDate(submittedQuestion.getCreationDate())
                    .userId(submittedQuestion.getUserId())
                    .build();

            // Add event to the gamification server
            apiUtils.postAskedAQuestionEvent(user.getId().asString());

            // Check if it's the user first question
            QuestionQuery query = QuestionQuery.builder().userId(user.getId()).build();

            QuestionsDTO res = this.getQuestions(query);
            if (res.getQuestions().size() == 1) {
                apiUtils.postFirstQuestionEvent(user.getId().asString());
            }

            return newQuestion;

        } catch (ApiFailException e) {
            e.printStackTrace();
            throw new QuestionFailedException("Error with gamification server");
        } catch (IOException e) {
            e.printStackTrace();
            throw new QuestionFailedException("Internal server error, retry later");
        } catch (Exception e) {
            throw new QuestionFailedException(e.getMessage());
        }
    }

    /**
     * Retrieve all question in the repo
     *
     * @return all questions as DTO
     */
    public QuestionsDTO getAllQuestions() {
        Collection<Question> allQuestions = questionRepository.findAll();

        return getQuestionsAsDTO(allQuestions, new ArrayList<>(), new ArrayList<>());
    }

    /**
     * Retrieve questions asked by the query
     *
     * @param query Query passed
     * @return return the result asked by the query as DTO
     * @throws QuestionFailedException
     */
    public QuestionsDTO getQuestions(QuestionQuery query) throws QuestionFailedException {

        if (query == null) {
            throw new QuestionFailedException("Query is null");
        } else {

            if (query.userId != null && query.title == null) {
                return getQuestionsAsDTO(questionRepository.findByUserId(query.userId), new ArrayList<>(),
                        new ArrayList<>());
            } else if (query.userId == null && query.title != null) {
                return getQuestionsAsDTO(questionRepository.findByTitleContaining(query.title), new ArrayList<>(),
                        new ArrayList<>());

            } else {
                throw new QuestionFailedException("Query invalid");
            }
        }

    }


    /**
     * Return a single Question asked by query
     *
     * @param query Query passed
     * @return the single question asked
     * @throws QuestionFailedException
     */
    public QuestionsDTO.QuestionDTO getQuestion(QuestionQuery query)
            throws QuestionFailedException, CommentFailedException, AnswerFailedException {

        QuestionsDTO.QuestionDTO questionFound;
        Question question;
        Collection<AnswersDTO.AnswerDTO> answersDTO = new ArrayList<>();
        Collection<CommentsDTO.CommentDTO> commentsDTO = new ArrayList<>();


        if (query == null) {
            throw new QuestionFailedException("Query is null");
        } else {

            if (query.getQuestionId() != null) {
                if (!query.isWithDetail()) {
                    question = questionRepository.findById(query.getQuestionId())
                            .orElseThrow(() -> new QuestionFailedException("The question hasn't been found"));
                } else { // Open a question page
                    try {
                        apiUtils.postOpenAQuestion(query.userId.asString());

                        question = questionRepository.findByIdWithAllDetails(query.getQuestionId())
                                .orElseThrow(() -> new QuestionFailedException("The question hasn't been found"));

                        answersDTO = getAnswers(question);
                        commentsDTO = commentFacade.getComments(
                                CommentQuery.builder()
                                        .questionId(question.getId())
                                        .build()
                        ).getComments();
                    } catch (ApiFailException e) {
                        e.printStackTrace();
                        throw new QuestionFailedException("Error with gamification server");
                    } catch (IOException e) {
                        e.printStackTrace();
                        throw new QuestionFailedException("Internal server error, retry later");
                    } catch (Exception e) {
                        throw new QuestionFailedException(e.getMessage());
                    }
                }

                questionFound = getQuestionAsDTO(question, answersDTO, commentsDTO);

            } else {
                throw new QuestionFailedException("Query invalid");
            }
        }

        return questionFound;
    }

    /**
     * Transform a collection of Question into DTO
     *
     * @param questionsFound collection of questions
     * @return Questions DTO
     */
    private QuestionsDTO getQuestionsAsDTO(Collection<Question> questionsFound,
                                           Collection<AnswersDTO.AnswerDTO> answers,
                                           Collection<CommentsDTO.CommentDTO> comments) {
        List<QuestionsDTO.QuestionDTO> QuestionsDTOFound =
                questionsFound.stream().map(
                        question -> getQuestionAsDTO(question, answers, comments)).collect(Collectors.toList());

        return QuestionsDTO.builder().questions(QuestionsDTOFound).build();
    }

    /**
     * Remove a question
     *
     * @param id the id of the question to be removed
     * @throws QuestionFailedException
     */
    public void removeQuestion(QuestionId id) throws QuestionFailedException {
        questionRepository.findById(id)
                .orElseThrow(() -> new QuestionFailedException("The question hasn't been found"));

        questionRepository.remove(id);
    }

    /**
     * Return the DTO of the question in the parameter
     *
     * @param question question to transform
     * @param answers  list of answer to the question
     * @return the DTO corresponding to the parameter
     */
    private QuestionsDTO.QuestionDTO getQuestionAsDTO(Question
                                                              question, Collection<AnswersDTO.AnswerDTO> answers,
                                                      Collection<CommentsDTO.CommentDTO> comments) {

        QuestionsDTO.QuestionDTO.QuestionDTOBuilder builder = QuestionsDTO.QuestionDTO.builder()
                .title(question.getTitle())
                .description(question.getDescription())
                .id(question.getId())
                .userId(question.getUserId())
                .votes(questionRepository.getVotes(question.getId()))
                .username(question.getUsername())
                .creationDate(question.getCreationDate());

        if (answers != null)
            builder.answersDTO(AnswersDTO.builder().answers(answers).build());

        if (comments != null)
            builder.commentsDTO(CommentsDTO.builder().comments(comments).build());


        return builder.build();
    }

    /**
     * Retrieve all answers associate to the question
     *
     * @param question Which question want the ansers
     * @return a collection of answer DTO associate to the question
     */
    private Collection<AnswersDTO.AnswerDTO> getAnswers(Question question) {
        return question.getAnswers().stream().map(
                answer -> {
                    Collection<CommentsDTO.CommentDTO> commentsDTO = new ArrayList<>();
                    try {
                        commentsDTO =
                                commentFacade.getComments(CommentQuery.builder().answerId(answer.getId()).build())
                                        .getComments();
                    } catch (CommentFailedException e) {
                        e.printStackTrace();
                    } catch (AnswerFailedException e) {
                        e.printStackTrace();
                    } catch (QuestionFailedException e) {
                        e.printStackTrace();
                    }


                    return AnswersDTO.AnswerDTO.builder()
                            .id(answer.getId())
                            .creationDate(answer.getCreationDate())
                            .description(answer.getDescription())
                            .username(answer.getUsername())
                            .comments(CommentsDTO.builder().comments(commentsDTO).build())
                            .build();
                }
        ).collect(Collectors.toList());
    }

    /**
     * Vote on a question
     *
     * @param userId     : id of the user voting
     * @param questionId : id of the question being voted
     * @param vote       : value that is being done (upvote / downvote)
     */
    public void vote(UserId userId, QuestionId questionId, int vote)
            throws QuestionFailedException, UserFailedException {

        checkIfUserExists(userId);

        int voteValue = questionRepository.getVoteValue(userId, questionId);

        // Update the vote value
        voteValue = getNewVoteValue(voteValue, vote);

        questionRepository.addVote(userId, questionId, voteValue);
    }


    /**
     * Checks if the given user id is linked to an actual user
     *
     * @param userId : id of the user we want to search
     * @throws QuestionFailedException if the user doesn't exist
     * @throws UserFailedException
     */
    private void checkIfUserExists(UserId userId) throws QuestionFailedException, UserFailedException {
        UsersDTO existingUser = userFacade.getUsers(UserQuery.builder().userId(userId).build());

        if (existingUser.getUsers().size() == 0)
            throw new QuestionFailedException("The user hasn't been found");
    }
}
