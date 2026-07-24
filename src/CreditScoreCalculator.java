import java.sql.*;
import java.util.*;

public class CreditScoreCalculator {

    private static final int MAX_SCORE = 1000;

    public static CreditScore calculate(int userId) {
        CreditScore score = new CreditScore();
        score.setUserId(userId);

        // 1. Gather all user financial data
        Map<String, Object> userData = gatherUserData(userId);

        // 2. Check if user has enough data
        if (!hasEnoughData(userData)) {
            score.setTotalScore(0);
            score.setRating("INSUFFICIENT DATA");
            score.setLoanLimit(0);
            score.setAIAnalysis("Not enough data. Start saving and using the app to generate your credit score.");
            return score;
        }

        // 3. Use AI to calculate credit score
        String aiResult = getAICreditScore(userData, userId);

        // 4. Parse AI response or use fallback
        if (aiResult != null && !aiResult.isEmpty()) {
            parseAIResponse(aiResult, score);
        } else {
            // Fallback to offline calculation if AI fails
            calculateOffline(score, userData);
        }

        // 5. Save to database
        saveToDatabase(score);

        return score;
    }

    private static Map<String, Object> gatherUserData(int userId) {
        Map<String, Object> data = new HashMap<>();

        try (Connection conn = SecureDatabaseConnection.connect()) {
            // 1. Get savings data
            PreparedStatement pst = conn.prepareStatement(
                    "SELECT COUNT(*) as total_deposits, " +
                            "COALESCE(SUM(amount), 0) as total_savings, " +
                            "COUNT(DISTINCT weekNO) as weeks_active, " +
                            "AVG(amount) as avg_deposit, " +
                            "MAX(amount) as max_deposit, " +
                            "MIN(amount) as min_deposit " +
                            "FROM mysaving2 WHERE user_id = ? AND amount > 0");
            pst.setInt(1, userId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                data.put("total_deposits", rs.getInt("total_deposits"));
                data.put("total_savings", rs.getDouble("total_savings"));
                data.put("weeks_active", rs.getInt("weeks_active"));
                data.put("avg_deposit", rs.getDouble("avg_deposit"));
                data.put("max_deposit", rs.getDouble("max_deposit"));
                data.put("min_deposit", rs.getDouble("min_deposit"));
            }

            // 2. Get expense data
            pst = conn.prepareStatement(
                    "SELECT COUNT(*) as total_withdrawals, " +
                            "COALESCE(SUM(ABS(amount)), 0) as total_expenses, " +
                            "AVG(ABS(amount)) as avg_expense " +
                            "FROM mysaving2 WHERE user_id = ? AND amount < 0");
            pst.setInt(1, userId);
            rs = pst.executeQuery();
            if (rs.next()) {
                data.put("total_withdrawals", rs.getInt("total_withdrawals"));
                data.put("total_expenses", rs.getDouble("total_expenses"));
                data.put("avg_expense", rs.getDouble("avg_expense"));
            }

            // 3. Get income vs expenses ratio
            pst = conn.prepareStatement(
                    "SELECT " +
                            "COALESCE(SUM(CASE WHEN amount > 0 THEN amount ELSE 0 END), 0) as income, " +
                            "COALESCE(SUM(CASE WHEN amount < 0 THEN ABS(amount) ELSE 0 END), 0) as expenses " +
                            "FROM mysaving2 WHERE user_id = ?");
            pst.setInt(1, userId);
            rs = pst.executeQuery();
            if (rs.next()) {
                data.put("total_income", rs.getDouble("income"));
                data.put("total_expenses_all", rs.getDouble("expenses"));
                double ratio = rs.getDouble("income") > 0 ?
                        (rs.getDouble("income") - rs.getDouble("expenses")) / rs.getDouble("income") : 0;
                data.put("savings_rate", ratio);
            }

            // 4. Get Chama data
            pst = conn.prepareStatement(
                    "SELECT COUNT(*) as chama_count, " +
                            "COALESCE(SUM(amount), 0) as total_chama_contributions " +
                            "FROM chama_contributions WHERE user_id = ?");
            pst.setInt(1, userId);
            rs = pst.executeQuery();
            if (rs.next()) {
                data.put("chama_count", rs.getInt("chama_count"));
                data.put("chama_contributions", rs.getDouble("total_chama_contributions"));
            }

            // 5. Get Debt data
            pst = conn.prepareStatement(
                    "SELECT COUNT(*) as total_debts, " +
                            "SUM(CASE WHEN status = 'PAID' THEN 1 ELSE 0 END) as paid_debts " +
                            "FROM debts WHERE user_id = ? AND type = 'I_OWE'");
            pst.setInt(1, userId);
            rs = pst.executeQuery();
            if (rs.next()) {
                data.put("total_debts", rs.getInt("total_debts"));
                data.put("paid_debts", rs.getInt("paid_debts"));
                double repayment = rs.getInt("total_debts") > 0 ?
                        (double) rs.getInt("paid_debts") / rs.getInt("total_debts") : 1.0;
                data.put("repayment_rate", repayment);
            }

            // 6. Get Activity data
            pst = conn.prepareStatement(
                    "SELECT COUNT(*) as total_activities " +
                            "FROM user_activity WHERE user_id = ? AND activity_time > DATE_SUB(NOW(), INTERVAL 30 DAY)");
            pst.setInt(1, userId);
            rs = pst.executeQuery();
            if (rs.next()) {
                data.put("activities_30days", rs.getInt("total_activities"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return data;
    }

    private static boolean hasEnoughData(Map<String, Object> data) {
        int deposits = (int) data.getOrDefault("total_deposits", 0);
        return deposits >= 3;
    }

    private static String getAICreditScore(Map<String, Object> data, int userId) {
        // Build prompt for AI
        String prompt = String.format(
                "You are a financial credit scoring AI for Africa (Kenya). " +
                        "Analyze this user's financial data and generate a credit score from 0-1000.\n\n" +
                        "USER DATA:\n" +
                        "- Total deposits: %d\n" +
                        "- Total savings: Ksh %.2f\n" +
                        "- Weeks active saving: %d\n" +
                        "- Average deposit: Ksh %.2f\n" +
                        "- Total income: Ksh %.2f\n" +
                        "- Total expenses: Ksh %.2f\n" +
                        "- Savings rate: %.2f%%\n" +
                        "- Chama contributions: %d\n" +
                        "- Total debts: %d\n" +
                        "- Debt repayment rate: %.2f%%\n" +
                        "- Activities in 30 days: %d\n\n" +
                        "CALCULATE:\n" +
                        "1. CREDIT_SCORE: (0-1000)\n" +
                        "2. RATING: (EXCELLENT/GOOD/FAIR/POOR/VERY POOR)\n" +
                        "3. LOAN_LIMIT: (based on savings and score)\n" +
                        "4. BREAKDOWN: \n" +
                        "   - Savings Score (0-300): \n" +
                        "   - Expense Score (0-250): \n" +
                        "   - Chama Score (0-150): \n" +
                        "   - Debt Score (0-150): \n" +
                        "   - Activity Score (0-150): \n\n" +
                        "Return in this format:\n" +
                        "SCORE: [number]\n" +
                        "RATING: [text]\n" +
                        "LOAN: [number]\n" +
                        "SAVINGS_SCORE: [number]\n" +
                        "EXPENSE_SCORE: [number]\n" +
                        "CHAMA_SCORE: [number]\n" +
                        "DEBT_SCORE: [number]\n" +
                        "ACTIVITY_SCORE: [number]\n" +
                        "ANALYSIS: [brief analysis of their financial behavior]",
                data.getOrDefault("total_deposits", 0),
                data.getOrDefault("total_savings", 0.0),
                data.getOrDefault("weeks_active", 0),
                data.getOrDefault("avg_deposit", 0.0),
                data.getOrDefault("total_income", 0.0),
                data.getOrDefault("total_expenses_all", 0.0),
                (double) data.getOrDefault("savings_rate", 0.0) * 100,
                data.getOrDefault("chama_count", 0),
                data.getOrDefault("total_debts", 0),
                (double) data.getOrDefault("repayment_rate", 0.0) * 100,
                data.getOrDefault("activities_30days", 0)
        );

        try {
            String response = GeminiClient.callGeminiAPI(prompt);
            return response;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void parseAIResponse(String response, CreditScore score) {
        try {
            // Parse the AI response
            String[] lines = response.split("\n");
            for (String line : lines) {
                if (line.startsWith("SCORE:")) {
                    score.setTotalScore(Integer.parseInt(line.substring(6).trim()));
                } else if (line.startsWith("RATING:")) {
                    score.setRating(line.substring(7).trim());
                } else if (line.startsWith("LOAN:")) {
                    score.setLoanLimit(Double.parseDouble(line.substring(5).trim()));
                } else if (line.startsWith("SAVINGS_SCORE:")) {
                    score.setSavingsScore(Double.parseDouble(line.substring(14).trim()));
                } else if (line.startsWith("EXPENSE_SCORE:")) {
                    score.setExpenseScore(Double.parseDouble(line.substring(14).trim()));
                } else if (line.startsWith("CHAMA_SCORE:")) {
                    score.setChamaScore(Double.parseDouble(line.substring(12).trim()));
                } else if (line.startsWith("DEBT_SCORE:")) {
                    score.setDebtScore(Double.parseDouble(line.substring(11).trim()));
                } else if (line.startsWith("ACTIVITY_SCORE:")) {
                    score.setActivityScore(Double.parseDouble(line.substring(15).trim()));
                } else if (line.startsWith("ANALYSIS:")) {
                    score.setAIAnalysis(line.substring(9).trim());
                }
            }

            // Ensure loan limit is reasonable
            double totalSavings = getTotalSavings(score.getUserId());
            double maxLoan = totalSavings * 2;
            if (score.getLoanLimit() > maxLoan) {
                score.setLoanLimit(maxLoan);
            }

        } catch (Exception e) {
            e.printStackTrace();
            // Fallback to offline calculation
            calculateOffline(score, null);
        }
    }

    private static void calculateOffline(CreditScore score, Map<String, Object> data) {
        // Simple offline calculation as fallback
        double savings = data != null ? (double) data.getOrDefault("total_savings", 0) : 0;
        int weeks = data != null ? (int) data.getOrDefault("weeks_active", 0) : 0;
        double rate = data != null ? (double) data.getOrDefault("savings_rate", 0) : 0;

        double savingsScore = Math.min(savings / 50, 300);
        double consistencyScore = Math.min(weeks * 10, 200);
        double rateScore = Math.min(rate * 500, 250);
        double chamaScore = 0;
        double debtScore = 0;
        double activityScore = 0;

        int total = (int)(savingsScore + consistencyScore + rateScore + chamaScore + debtScore + activityScore);
        score.setTotalScore(Math.min(total, MAX_SCORE));
        score.setRating(getRating(score.getTotalScore()));
        score.setLoanLimit(Math.min(savings * 0.5, 50000));
        score.setAIAnalysis("AI analysis unavailable. Score calculated using standard rules.");
    }

    private static double getTotalSavings(int userId) {
        try (Connection conn = SecureDatabaseConnection.connect()) {
            PreparedStatement pst = conn.prepareStatement(
                    "SELECT COALESCE(SUM(amount), 0) FROM mysaving2 WHERE user_id = ? AND amount > 0");
            pst.setInt(1, userId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                return rs.getDouble(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private static String getRating(int score) {
        if (score >= 800) return "EXCELLENT";
        if (score >= 600) return "GOOD";
        if (score >= 400) return "FAIR";
        if (score >= 200) return "POOR";
        if (score > 0) return "VERY POOR";
        return "INSUFFICIENT DATA";
    }

    private static void saveToDatabase(CreditScore score) {
        try (Connection conn = SecureDatabaseConnection.connect();
             PreparedStatement pst = conn.prepareStatement(
                     "INSERT INTO credit_scores (user_id, score, rating, loan_limit, " +
                             "savings_score, expense_score, chama_score, debt_score, activity_score, ai_analysis) " +
                             "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            pst.setInt(1, score.getUserId());
            pst.setInt(2, score.getTotalScore());
            pst.setString(3, score.getRating());
            pst.setDouble(4, score.getLoanLimit());
            pst.setDouble(5, score.getSavingsScore());
            pst.setDouble(6, score.getExpenseScore());
            pst.setDouble(7, score.getChamaScore());
            pst.setDouble(8, score.getDebtScore());
            pst.setDouble(9, score.getActivityScore());
            pst.setString(10, score.getAIAnalysis());
            pst.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}