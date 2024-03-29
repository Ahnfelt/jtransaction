package jtransaction;

public class Retry {
    public static void main(String[] arguments) {


        final Slot<Boolean> done = Record.slot(false);


        new Thread(new Runnable(){public void run(){
            try {

                Thread.sleep(5000);
                done.set(true);

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }}).start();


        new Thread(new Runnable(){public void run(){

            Transaction.run(new Runnable(){public void run(){

                // Normally non-transactional calls like this inside a transaction
                // would be a bug, but in this case we actually want to observe
                // how many times the transaction is tried (likely 2 times)
                System.out.println("Trying...");

                if(!done.get()) Transaction.retry();

            }});

            System.out.println("Done waiting for the other thread!");

        }}).start();
    }
}
