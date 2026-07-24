// SimpleContribution.java
import java.util.Date;

public class SimpleContribution {
    private int id;
    private int chamaId;
    private int memberId;
    private double amount;
    private String paymentMethod;
    private String transactionId;
    private Date contributionDate;
    private int recordedBy;

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getChamaId() { return chamaId; }
    public void setChamaId(int chamaId) { this.chamaId = chamaId; }

    public int getMemberId() { return memberId; }
    public void setMemberId(int memberId) { this.memberId = memberId; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public Date getContributionDate() { return contributionDate; }
    public void setContributionDate(Date contributionDate) { this.contributionDate = contributionDate; }

    public int getRecordedBy() { return recordedBy; }
    public void setRecordedBy(int recordedBy) { this.recordedBy = recordedBy; }
}