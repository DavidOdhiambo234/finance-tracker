import java.sql.Date;

// SimpleMember.java
public class SimpleMember {
    private int id;
    private int chamaId;
    private String fullname;
    private String phoneNumber;
    private String mpesaNumber;
    private String memberCode;
    private Date joinDate;
    private boolean isRegistered;
    private Integer registeredUserId;
    private double totalContributions;
    private String status;
    private Date createdAt;

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getChamaId() { return chamaId; }
    public void setChamaId(int chamaId) { this.chamaId = chamaId; }

    public String getFullname() { return fullname; }
    public void setFullname(String fullname) { this.fullname = fullname; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getMpesaNumber() { return mpesaNumber; }
    public void setMpesaNumber(String mpesaNumber) { this.mpesaNumber = mpesaNumber; }

    public String getMemberCode() { return memberCode; }
    public void setMemberCode(String memberCode) { this.memberCode = memberCode; }

    public Date getJoinDate() { return joinDate; }
    public void setJoinDate(Date joinDate) { this.joinDate = joinDate; }

    public boolean isRegistered() { return isRegistered; }
    public void setRegistered(boolean registered) { isRegistered = registered; }

    public Integer getRegisteredUserId() { return registeredUserId; }
    public void setRegisteredUserId(Integer registeredUserId) { this.registeredUserId = registeredUserId; }

    public double getTotalContributions() { return totalContributions; }
    public void setTotalContributions(double totalContributions) { this.totalContributions = totalContributions; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}