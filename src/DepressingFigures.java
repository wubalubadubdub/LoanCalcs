import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by bearg on 5/5/2016.
 * This class does various calculations regarding loans.
 * The getDaysFromMonth method assumes 28 days in February at present.
 */
public class DepressingFigures {

    // first param is number of sig figs to use. need to give MathContext so BigDecimal divide method
    // doesn't throw an exception if the result is an non-terminating decimal
    private static final MathContext PRECISION_AND_ROUNDING = new MathContext(10, RoundingMode.HALF_EVEN);

    // don't pass double 0.05125 in directly -- pass it as a String
    // 5.125% interest rate, divided by 365.25 to get daily rate and again by 100 to get a decimal
    private static final BigDecimal INTEREST_RATE = new BigDecimal("0.05125")
            .divide(new BigDecimal("365.25"), PRECISION_AND_ROUNDING);



    private BigDecimal principal;
    private int currentMonth;

    public DepressingFigures(BigDecimal principal, int currentMonth) {

        this.principal = principal;
        this.currentMonth = currentMonth;
    }

    private BigDecimal comparePayments(BigDecimal payment1, BigDecimal payment2) {

        return null;


    }

    private BigDecimal nextMonthBalance() {

        BigDecimal interest = this.monthlyInterestAccumulated();
        return principal.add(interest);
    }

    private BigDecimal monthlyInterestAccumulated() {

        int days;

        days = getDaysFromMonth(this.currentMonth);


        BigDecimal amount = principal.multiply(INTEREST_RATE).multiply(new BigDecimal(days));
        amount = amount.setScale(2, RoundingMode.CEILING);

        return amount;
    }

    /**
     *
     * @param days the number of days since last payment was made. used to calculate the accumlated interest
     *             since then
     * @return the dollar amount of interest that has accumulated in the last N days since last payment
     */
    private BigDecimal interestAccumulated(int days) {

        BigDecimal amount = principal.multiply(INTEREST_RATE).multiply(new BigDecimal(days));
        amount = amount.setScale(2, RoundingMode.CEILING);

        return amount;
    }

    private int getDaysFromMonth(final int month) {

        final int days;

        List<Integer> monthsWith30Days = new ArrayList<>();
        monthsWith30Days.add(4);
        monthsWith30Days.add(6);
        monthsWith30Days.add(9);
        monthsWith30Days.add(11);

        List<Integer> monthsWith31Days = new ArrayList<>();
        monthsWith31Days.add(1);
        monthsWith31Days.add(3);
        monthsWith31Days.add(5);
        monthsWith31Days.add(7);
        monthsWith31Days.add(8);
        monthsWith31Days.add(10);

        // not included in the above is February, which we'll handle separately

        if (monthsWith31Days.contains(month)) {

            days = 31;
        }

        else if (monthsWith30Days.contains(month)) {

            days = 30;
        }

        else if (month == 2) { // February

            days = 28;
        }

        else {

            throw new IllegalArgumentException("Not a valid month. Must be a number from 1 to 12");
        }

        return days;

    }


    public static void main(String[] args) {

        DepressingFigures df = new DepressingFigures(new BigDecimal("5512.09"), 4);
        BigDecimal monthlyInterest = df.monthlyInterestAccumulated();
        BigDecimal interest = df.interestAccumulated(16);
        BigDecimal nextMonthBalance = df.nextMonthBalance();

        System.out.println(monthlyInterest);
        System.out.println(interest);
        System.out.println(nextMonthBalance);

    }
}
