import java.util.Date;

public class CreditScore {
    private int userId;
    private Date date;
    private double savingsScore;
    private double expenseScore;
    private double chamaScore;
    private double debtScore;
    private double activityScore;
    private int totalScore;
    private double loanLimit;
    private String rating;
    private String aiAnalysis;

    public CreditScore() {
        this.date = new Date();
    }

    // Getters
    public int getUserId() { return userId; }
    public Date getDate() { return date; }
    public double getSavingsScore() { return savingsScore; }
    public double getExpenseScore() { return expenseScore; }
    public double getChamaScore() { return chamaScore; }
    public double getDebtScore() { return debtScore; }
    public double getActivityScore() { return activityScore; }
    public int getTotalScore() { return totalScore; }
    public double getLoanLimit() { return loanLimit; }
    public String getRating() { return rating; }
    public String getAIAnalysis() { return aiAnalysis; }

    // Setters
    public void setUserId(int userId) { this.userId = userId; }
    public void setDate(Date date) { this.date = date; }
    public void setSavingsScore(double savingsScore) { this.savingsScore = savingsScore; }
    public void setExpenseScore(double expenseScore) { this.expenseScore = expenseScore; }
    public void setChamaScore(double chamaScore) { this.chamaScore = chamaScore; }
    public void setDebtScore(double debtScore) { this.debtScore = debtScore; }
    public void setActivityScore(double activityScore) { this.activityScore = activityScore; }
    public void setTotalScore(int totalScore) { this.totalScore = totalScore; }
    public void setLoanLimit(double loanLimit) { this.loanLimit = loanLimit; }
    public void setRating(String rating) { this.rating = rating; }
    public void setAIAnalysis(String aiAnalysis) { this.aiAnalysis = aiAnalysis; }
}