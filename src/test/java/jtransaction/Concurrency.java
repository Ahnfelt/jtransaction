package jtransaction;

public class Concurrency {

    public interface Person {

        public long getCredits();
        public void setCredits(long value);

    }

    public static void main(String[] arguments) {

        final Person anna = Record.create(Person.class);
        anna.setCredits(0);

        for(int i = 0; i < 50; i++) {

            new Thread(new Runnable(){public void run(){

                Transaction.run(new Runnable(){public void run(){

                    anna.setCredits(anna.getCredits() - 2);

                    if(anna.getCredits() < 0 || anna.getCredits() > 2) {
                        Transaction.retry();
                    }

                }});

                if(anna.getCredits() < 0) {
                    System.out.println("The transactions aren't working!");
                }

            }}).start();

        }

        for(int i = 0; i < 100; i++) {

            new Thread(new Runnable(){public void run(){

                Transaction.run(new Runnable(){public void run(){

                    anna.setCredits(anna.getCredits() + 1);

                    if(anna.getCredits() < 0 || anna.getCredits() > 2) {
                        Transaction.retry();
                    }

                }});

                if(anna.getCredits() < 0) {
                    System.out.println("The transactions aren't working!");
                }

            }}).start();

        }
    }
}
