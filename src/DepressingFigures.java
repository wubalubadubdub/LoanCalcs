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
    private static final MathContext SIG_FIGS_AND_ROUNDING = new MathContext(20, RoundingMode.HALF_EVEN);


    // don't pass double 0.05125 in directly -- pass it as a String
    // 5.125% interest rate, divided by 365.25 to get daily rate and again by 100 to get a decimal
    private static final BigDecimal INTEREST_RATE = new BigDecimal("0.05125")
            .divide(new BigDecimal("365.25"), SIG_FIGS_AND_ROUNDING);



    private BalancePair mBalancePair;
    private int mCurrentMonth;

    public int getmCurrentMonth() {
        return mCurrentMonth;
    }

    public DepressingFigures(BalancePair pair, int currentMonth) {

        this.mBalancePair = pair;
        this.mCurrentMonth = currentMonth;
    }

    public BalancePair getBalancePair() {

        return mBalancePair;
    }

    public BigDecimal comparePayments(BigDecimal payment1, BigDecimal payment2) {

        return null;


    }

    /**
     *
     * @return balance after a month of interest accumulation
     */
    public BalancePair nextMonthBalance(int monthNumber) {

        // added accumulated interest to starting interest
        BigDecimal interest = mBalancePair.getInterest()
                .add(monthlyInterestAccumulated(monthNumber), SIG_FIGS_AND_ROUNDING);
        BigDecimal principal = mBalancePair.getPrincipal();

        return new BalancePair(principal, interest);
    }

    public BigDecimal monthlyInterestAccumulated(int monthNumber) {

        int days;

        days = getDaysFromMonth(monthNumber);


        BigDecimal amount = mBalancePair.getPrincipal()
                .multiply(INTEREST_RATE, SIG_FIGS_AND_ROUNDING)
                .multiply(new BigDecimal(days), SIG_FIGS_AND_ROUNDING);

        amount = amount.setScale(2, RoundingMode.CEILING);

        return amount;
    }

    /**
     *
     * @param daysSinceLastPayment the number of daysSinceLastPayment since last payment was made. used to calculate the accumlated interest
     *             since then
     * @return the dollar amount of interest that has accumulated in the last N daysSinceLastPayment since last payment
     */
    private BigDecimal interestAccumulated(int daysSinceLastPayment) {

        BigDecimal amount = mBalancePair.getPrincipal()
                .multiply(INTEREST_RATE, SIG_FIGS_AND_ROUNDING)
                .multiply(new BigDecimal(daysSinceLastPayment), SIG_FIGS_AND_ROUNDING);

        amount = amount.setScale(2, RoundingMode.CEILING);

        return amount;
    }

    public int getDaysFromMonth(final int month) {

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

    public void makePayment(BigDecimal payment) {



        // apply payment to interest first
        final BigDecimal interest = mBalancePair.getInterest();

        if (payment.compareTo(interest) < 1) { // payment is less than or equal to current interest

            BigDecimal newInterest = interest.subtract(payment, SIG_FIGS_AND_ROUNDING);
            mBalancePair.setInterest(newInterest);
        }

        else {
            // calculate amount to apply to principal
            BigDecimal appliedToPrincipal = payment.subtract(interest, SIG_FIGS_AND_ROUNDING);

            // payment was more than interest -- set it to $0.00
            mBalancePair.setInterest(new BigDecimal("0.00"));

            // get current principal, subtract the amount from it
            BigDecimal currentPrincipal = mBalancePair.getPrincipal();
            mBalancePair.setPrincipal(currentPrincipal.subtract(appliedToPrincipal, SIG_FIGS_AND_ROUNDING));
        }

    }

    public void makePaymentSeries(BigDecimal payment, int monthsToPay) {

        BigDecimal currentPrincipal = mBalancePair.getPrincipal();


        int currentMonth = getmCurrentMonth(); // used by monthlyInterestAccumulated
        // to get the number of days before next payment due date

        // number of months we've made the payment
        int monthsPaid = 0;


        while (monthsToPay > 0) {

            makePayment(payment); // interest should be recalculated after we make a payment
            BigDecimal currentInterest = mBalancePair.getInterest();

            monthsToPay--;

            // need to calculate interest that accumulates in the month between payments
            // and set the interest to be that amount + the current interest
            mBalancePair.setInterest(currentInterest
                    .add(monthlyInterestAccumulated(currentMonth), SIG_FIGS_AND_ROUNDING));



           currentMonth++;

            // update count of elapsed months by 1
            monthsPaid++;

            System.out.println("Balance in " + monthsPaid + " month(s): \n" + mBalancePair + "\n");
        }

    }


    public static void main(String[] args) {

        /*DepressingFigures df = new DepressingFigures(new BalancePair(
                new BigDecimal("5512.09"), new BigDecimal("12.38")
                ), 5);

        BigDecimal payment = new BigDecimal("200.00");
        df.makePaymentSeries(payment, 6);*/

        DepressingFigures df = new DepressingFigures(new BalancePair(
                new BigDecimal("5500"), new BigDecimal("12.00")), 5);

        BigDecimal payment = new BigDecimal("20.00");
        df.makePaymentSeries(payment, 6);










    }
}
