
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.BiPredicate;

/**
 * Created by bearg on 5/5/2016.
 * This class does various calculations regarding loans.
 * The getDaysFromMonth method assumes 28 days in February at present.
 * TODO: Add ability to track track total interest paid
 */
public class DepressingFigures {

    // first param is number of sig figs to use. need to give MathContext so BigDecimal divide method
    // doesn't throw an exception if the result is an non-terminating decimal
    private static final MathContext SIG_FIGS_AND_ROUNDING = new MathContext(20, RoundingMode.HALF_EVEN);


    // don't pass double 0.05125 in directly -- pass it as a String
    // 5.125% interest rate, divided by 365.25 to get daily rate and again by 100 to get a decimal
    private static final BigDecimal INTEREST_RATE = new BigDecimal("0.05125")
            .divide(new BigDecimal("365.25"), SIG_FIGS_AND_ROUNDING);

    private static final BigDecimal EPSILON = new BigDecimal("1.00");
    // be within this dollar amount of $0.00 at the end
    // for both the principal and interest due

    private static final String MODE_OPTIONS_HELP =
            "minpay: provide the number of months that you want to pay off the loan in." +
                    " This calculates the minimum monthly payment you need to make to do that.\n\n" +
                    "payseries: provide a payment to make and the number of months to make it, and this shows" +
                    " you the balance at the end of each month until then.\n\n" +
                    "nextbal: next month's balance if you let interest accumulate the entire month\n\n" +
                    "bipay: provide a payment amount that will be split into two equal amounts, one to be" +
                    " paid on the 15th and the other on the 5th of the following month. displays the balance " +
                    "after the second payment has been made, i.e. balance after payment on 5th of following month\n\n" +
                    "compare: compare splitting a payment into two and paying bimonthly vs. paying that amount at the " +
                    "end of the cycle. see how much will be saved in interest\n\n";



    private BalancePair mBalancePair;
    private int mCurrentMonth;

    public int getmCurrentMonth() {
        return mCurrentMonth;
    }

    public DepressingFigures(BalancePair pair) {

        mBalancePair = pair;
        mCurrentMonth = Calendar.getInstance().get(Calendar.MONTH);
        mCurrentMonth++; // Calendar is 0 indexed, so we must add 1 to the actual month number
    }

    public BalancePair getBalancePair() {

        return mBalancePair;
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


    /**
     *
     * @param monthNumber a number 1-12 that represents that month
     *                    in the calendar year
     * @return the interest that accumulates between monthly payments
     */
    public BigDecimal monthlyInterestAccumulated(int monthNumber) {

        int days;

        days = getDaysFromMonth(monthNumber);

        return mBalancePair.getPrincipal()
                .multiply(INTEREST_RATE, SIG_FIGS_AND_ROUNDING)
                .multiply(new BigDecimal(days), SIG_FIGS_AND_ROUNDING);
    }

    /**
     *
     * @param daysSinceLastPayment the number of daysSinceLastPayment since last payment was made.
     *                             used to calculate the accumulated interest
     *                             since then
     * @return the dollar amount of interest that has accumulated in the last N daysSinceLastPayment since last payment
     */
    private BigDecimal interestAccumulated(int daysSinceLastPayment) {

        return mBalancePair.getPrincipal()
                .multiply(INTEREST_RATE, SIG_FIGS_AND_ROUNDING)
                .multiply(new BigDecimal(daysSinceLastPayment), SIG_FIGS_AND_ROUNDING);
    }

    /**
     *
     *
     * @param month a number 1-12 that represents that month
     *                    in the calendar year
     * @return the number of days in that month
     */
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
        monthsWith31Days.add(12);


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

    /**
     * Shows the balance 1 month from now if you make the payment on the current date
     * @param payment the payment amount as a BigDecimal
     */
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

    /**
     * This method calculates and prints the principal and interest balance after each month for the
     * number of months specified when the payment is applied each month.
     * @param payment the amount to be applied each month
     * @param monthsToPay the number of months to apply the payment
     */
    public void makePaymentSeries(BigDecimal payment, int monthsToPay) {

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

            if (currentMonth == 12) {

                currentMonth = 1;
            }

            else {
                currentMonth++;
            }

            // update count of elapsed months by 1
            monthsPaid++;

            System.out.println("Balance in " + monthsPaid + " month(s): \n" + mBalancePair + "\n");

            // if balance goes lower than EPSILON less than 0.00, stop making payments
            BigDecimal amountBelowZero = new BigDecimal("0.00")
                    .subtract(mBalancePair.getPrincipal(), SIG_FIGS_AND_ROUNDING);

            if (amountBelowZero.compareTo(EPSILON) > 0) {

                System.out.println("Loan is fully paid after month " + (monthsPaid - 1));
                break;
            }
        }

    }

    /**
     * Calculates the minimum monthly payment needed to pay off the loan within the
     * specified number of months.
     * @param monthsToPayoff the number of months to fully pay off the balance
     * @return the minimum monthly payment needed
     */
    public BigDecimal monthlyPaymentNeeded(int monthsToPayoff) {



        int startingMonth = mCurrentMonth;
        BigDecimal guessPayment;

        // need to choose a lower and upper bound for the needed monthly payment,
        // then binary search for the exact payment

        BigDecimal finalAmount;
        BigDecimal startingPrincipal = mBalancePair.getPrincipal();
        BigDecimal startingInterest = mBalancePair.getInterest();

        // low payment can be the amount we'd pay if we just covered the interest every month
        // this varies slightly depending on the month, but we just need an approximate figure anyway
        BigDecimal lowPayment = mBalancePair.getInterest();

        // high payment can be the current principal
        BigDecimal highPayment = mBalancePair.getPrincipal();

        // this bipredicate returns true if the ending amount is within EPSILON of $0.00.
        // thus, our stopping condition will be abs(finalValue) <= EPSILON, or - EPSILON <= finalValue <= + EPSILON
        BiPredicate<BigDecimal, BigDecimal> withinEpsilon = (v, e) -> v.abs().compareTo(e) <= 0;


        while (true) {

            // need to reset the starting amounts for each new guess
            mBalancePair.setPrincipal(startingPrincipal);
            mBalancePair.setInterest(startingInterest);
            int currentMonth = startingMonth;

            guessPayment = lowPayment.add(highPayment, SIG_FIGS_AND_ROUNDING )
                    .divide(new BigDecimal(2), SIG_FIGS_AND_ROUNDING);

            System.out.printf("\nTrying amount $ %.2f for %d months\n", guessPayment, monthsToPayoff);

            makePaymentSeries(guessPayment, monthsToPayoff);


            if ((currentMonth + monthsToPayoff) > 12) {
                currentMonth = (currentMonth + monthsToPayoff) % 12;
            } else {
                currentMonth += monthsToPayoff;
            }

            finalAmount = nextMonthBalance(currentMonth).getPrincipal();

            if (withinEpsilon.test(finalAmount, EPSILON)) { // we're done, break from the loop

                // reset the values to what they were to begin with
                mBalancePair.setPrincipal(startingPrincipal);
                mBalancePair.setInterest(startingInterest);
                mCurrentMonth = startingMonth;

                return guessPayment;

            } else {

                if (finalAmount.compareTo(EPSILON) > 0) { // final amount > EPSILON, meaning payment was too low

                    lowPayment = guessPayment;

                } else { // final amount < EPSILON, meaning payment was too high

                    highPayment = guessPayment;
                }
            }


        }

    }

    public void printMinMonthlyPayment(BigDecimal minMonthlyPayment, int monthsToPayOff) {

        System.out.printf("The minimum monthly payment is $ %.2f " +
        "for the loan to be paid off in " +
        monthsToPayOff + " months\n", minMonthlyPayment);
    }

    public void makeBiMonthlyPayments(BigDecimal payment) {

        BigDecimal halfPayment = payment.divide(new BigDecimal(2), SIG_FIGS_AND_ROUNDING);

        int currentMonth = getmCurrentMonth();
        int days = getDaysFromMonth(currentMonth);

        if (days == 30) {


            BigDecimal interestToAdd = interestAccumulated(10); // from 5th to 15th of month

            // add the accumulated interest to the current interest
            getBalancePair().setInterest(getBalancePair().getInterest().add(interestToAdd));

            System.out.println("Making payment on 15th with balance " + mBalancePair);

            makePayment(halfPayment); // 1st payment, on 15th of month
            interestToAdd = interestAccumulated(20); // from 15th to 5th, 30 day month
            getBalancePair().setInterest(getBalancePair().getInterest().add(interestToAdd));

            System.out.println("Making payment on 5th with balance " + mBalancePair);

            makePayment(halfPayment); // 2nd payment, on 5th of following month

            System.out.println("Balance after payment on 5th: " + mBalancePair);

        } else if (days == 31) {

            BigDecimal interestToAdd = interestAccumulated(10); // from 5th to 15th of month

            // add the accumulated interest to the current interest
            getBalancePair().setInterest(getBalancePair().getInterest().add(interestToAdd));

            System.out.println("Making payment on 15th with balance " + mBalancePair);

            makePayment(halfPayment); // 1st payment, on 15th of month
            interestToAdd = interestAccumulated(21); // from 15th to 5th, 31 day month
            getBalancePair().setInterest(getBalancePair().getInterest().add(interestToAdd));

            System.out.println("Making payment on 5th with balance " + mBalancePair);

            makePayment(halfPayment); // 2nd payment, on 5th of following month

            System.out.println("Balance after payment on 5th: " + mBalancePair);

        } else { // 28 days -- February

            BigDecimal interestToAdd = interestAccumulated(10); // from 5th to 15th of month

            // add the accumulated interest to the current interest
            getBalancePair().setInterest(getBalancePair().getInterest().add(interestToAdd));

            System.out.println("Making payment on 15th with balance \n" + mBalancePair);

            makePayment(halfPayment); // 1st payment, on 15th of month
            interestToAdd = interestAccumulated(18); // from 15th to 5th, 28 day month
            getBalancePair().setInterest(getBalancePair().getInterest().add(interestToAdd));

            System.out.println("Making payment on 5th with balance \n" + mBalancePair);

            makePayment(halfPayment); // 2nd payment, on 5th of following month

            System.out.println("Balance after payment on 5th: " + mBalancePair);

        }

    }

    public void compareMonthlyVsBimonthly(BigDecimal payment) {

        int days = getDaysFromMonth(mCurrentMonth);

        BigDecimal startingPrincipal = mBalancePair.getPrincipal();
        BigDecimal startingInterest = mBalancePair.getInterest();

        makeBiMonthlyPayments(payment);
        BigDecimal afterBimonthly = mBalancePair.getPrincipal();

        System.out.println("After bimonthly principal: " + afterBimonthly);

        // reset the balance before making the same payment, but in one instead of split into two
        mBalancePair.setPrincipal(startingPrincipal);

        // add the interest accumulated over the whole month to the starting interest
        BigDecimal interestToAdd = interestAccumulated(days);
        mBalancePair.setInterest(startingInterest.add(interestToAdd, SIG_FIGS_AND_ROUNDING));

        // apply the single monthly payment
        makePayment(payment);
        BigDecimal afterMonthly = mBalancePair.getPrincipal();

        System.out.println("After monthly principal: " + afterMonthly);

        // compare -- show interest saved and extra principal reduction
        // difference in amount paid in interest is the difference between the two principals
        BigDecimal principalDiffBimonthly = startingPrincipal
                .subtract(afterBimonthly, SIG_FIGS_AND_ROUNDING);

        BigDecimal principalDiffMonthly = startingPrincipal
                .subtract(afterMonthly, SIG_FIGS_AND_ROUNDING);

        System.out.printf("\nPrincipal reduction with bimonthly payments: $%.2f", principalDiffBimonthly);
        System.out.printf("\nPrincipal reduction with monthly payments: $%.2f", principalDiffMonthly);

        BigDecimal relativeDifference = principalDiffBimonthly.subtract(principalDiffMonthly, SIG_FIGS_AND_ROUNDING);

        System.out.printf("\nBy splitting $%.2f in half and paying 2x a month, you saved $%.2f in interest"
                , payment, relativeDifference);
    }


    public static void main(String[] args) {

        DepressingFigures df;

        BigDecimal principal;
        BigDecimal interest;

        List<String> modeOptions = new ArrayList<>();
        String[] saModeOptions = {"help", "minpay", "payseries", "nextbal", "bipay", "compare"};
        for (String s : saModeOptions) {

            modeOptions.add(s);
        }

        Scanner sc = new Scanner(System.in);

        while (true) { // loop until input is valid

            try {

                System.out.println("Enter principal amount: ");
                String principalStr = sc.next();
                principal = new BigDecimal(principalStr);

                System.out.println("Enter interest amount: ");
                String interestStr = sc.next();
                interest = new BigDecimal(interestStr);

                BalancePair bp = new BalancePair(principal, interest);
                df = new DepressingFigures(bp);


                System.out.println("Enter mode, or to see a list of available modes, " +
                        "type \"help\" : ");

                String modeStr = sc.next();

                if (!modeOptions.contains(modeStr)) {

                    throw new IllegalArgumentException();
                }

                switch (modeStr) {

                    case "help":
                        System.out.println(MODE_OPTIONS_HELP);
                        break;

                    case "minpay":
                        System.out.println("Enter months to pay off loan in: ");
                        int monthsToPayOff = sc.nextInt();
                        BigDecimal minPayment = df.monthlyPaymentNeeded(monthsToPayOff);
                        df.printMinMonthlyPayment(minPayment, monthsToPayOff);
                        break;

                    case "payseries":
                        System.out.println("Enter payment amount to make each month: ");
                        String paymentStr = sc.next();
                        BigDecimal payment = new BigDecimal(paymentStr);

                        System.out.println("Enter number of months to make this payment: ");
                        int monthsToPay = sc.nextInt();

                        df.makePaymentSeries(payment, monthsToPay);
                        break;

                    case "nextbal":
                        System.out.println(df.nextMonthBalance(df.getmCurrentMonth()));
                        break;

                    case "bipay":
                        System.out.println("Enter payment to be split into two bi-monthly payments: ");
                        String biPaymentStr = sc.next();
                        BigDecimal biPayment = new BigDecimal(biPaymentStr);

                        df.makeBiMonthlyPayments(biPayment);
                        break;

                    case "compare":
                        System.out.println("Enter payment amount for comparison: ");
                        String comparisonStr = sc.next();
                        BigDecimal comparisonPayment = new BigDecimal(comparisonStr);

                        df.compareMonthlyVsBimonthly(comparisonPayment);
                        break;

                }


                break;

            } catch (NumberFormatException nfe) {

                System.out.println("Not a valid number.");

            } catch (IllegalArgumentException iae) {

                System.out.println("Not a valid mode. " +
                        "Type \"help\" to get a list of modes and what they do.");

            } catch (Exception e) {

                System.out.println("Invalid input parameter");
            }
        }

    }
}
