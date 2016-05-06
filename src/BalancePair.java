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

        BigDecimal principal = getPrincipal();
        BigDecimal interest = getInterest();
        principal = principal.setScale(2, BigDecimal.ROUND_HALF_EVEN);
        interest = interest.setScale(2, BigDecimal.ROUND_HALF_EVEN);

        return "Principal: $ " + principal +
                "\nInterest: $ " + interest;

    }

    public void setPrincipal(final BigDecimal principal) {
        this.principal = principal;
    }

    public void setInterest(final BigDecimal interest) {
        this.interest = interest;
    }
}
