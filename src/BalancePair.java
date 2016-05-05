import java.math.BigDecimal;

/**
 * Created by bearg on 5/5/2016.
 */
public class BalancePair {

    private BigDecimal principal;
    private BigDecimal interest;

    public BalancePair(BigDecimal principal, BigDecimal interest) {

        this.principal = principal;
        this.interest = interest;
    }

    public BigDecimal getPrincipal() {
        return principal;
    }

    public BigDecimal getInterest() {
        return interest;
    }

    @Override
    public String toString() {

        return "Principal: $ " +
                getPrincipal() +
                "\n" +
                "Interest: $ " +
                getInterest();
    }

    public void setPrincipal(final BigDecimal principal) {
        this.principal = principal;
    }

    public void setInterest(final BigDecimal interest) {
        this.interest = interest;
    }
}
