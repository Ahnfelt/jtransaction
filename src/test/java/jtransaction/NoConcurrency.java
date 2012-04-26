package jtransaction;

public class NoConcurrency {

    public interface Person {

        public long getCredits();
        public void setCredits(long value);

    }

    public static void main(String[] arguments) {

        final Person anna = Record.create(Person.class);
        final Person birgit = Record.create(Person.class);


        Transaction.run(new Runnable(){public void run(){

            anna.setCredits(500);
            birgit.setCredits(0);

        }});


        final Slot<Integer> slot = Record.slot(0);
        final long amount = 200;

        Transaction.run(new Runnable(){public void run(){

            slot.set(10);
            if(anna.getCredits() >= amount) {
                anna.setCredits(anna.getCredits() - amount);
                birgit.setCredits(birgit.getCredits() + amount);
            } else {
                Transaction.rollback();
            }

        }});


        System.out.println("Slot:" + slot.get());
        System.out.println("Anna's credits:   " + anna.getCredits());
        System.out.println("Birgit's credits: " + birgit.getCredits());

    }


}
