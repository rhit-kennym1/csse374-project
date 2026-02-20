package testclasses.example;

public class TestSingleResponsibilityPrinciple {

    static class UserProfile {
        private String firstName;
        private String lastName;
        private String email;

        public UserProfile(String firstName, String lastName, String email) {
            this.firstName = firstName;
            this.lastName = lastName;
            this.email = email;
        }

        public String getFullName() {
            return firstName + " " + lastName;
        }

        public String getEmailLine() {
            return firstName + " <" + email + ">";
        }

        public void setEmail(String e) {
            this.email = e;
        }
    }

    static class GodClass {
        private String state;

        public void m01() {
            state = "01";
        }

        public void m02() {
            state = "02";
        }

        public void m03() {
            state = "03";
        }

        public void m04() {
            state = "04";
        }

        public void m05() {
            state = "05";
        }

        public void m06() {
            state = "06";
        }

        public void m07() {
            state = "07";
        }

        public void m08() {
            state = "08";
        }

        public void m09() {
            state = "09";
        }

        public void m10() {
            state = "10";
        }

        public void m11() {
            state = "11";
        }

        public void m12() {
            state = "12";
        }

        public void m13() {
            state = "13";
        }

        public void m14() {
            state = "14";
        }

        public void m15() {
            state = "15";
        }

        public void m16() {
            state = "16";
        }

        public void m17() {
            state = "17";
        }

        public void m18() {
            state = "18";
        }

        public void m19() {
            state = "19";
        }

        public void m20() {
            state = "20";
        }

        public void m21() {
            state = "21";
        }
    }

    static class DataHoarder {
        private String field01;
        private String field02;
        private String field03;
        private String field04;
        private String field05;
        private String field06;
        private String field07;
        private String field08;
        private String field09;
        private String field10;
        private String field11;
        private String field12;
        private String field13;
        private String field14;
        private String field15;
        private String field16;

        public void store(String v) {
            field01 = v;
        }
    }

    static class HelperA {
        public void help() {
        }
    }

    static class HelperB {
        public void help() {
        }
    }

    static class HelperC {
        public void help() {
        }
    }

    static class HelperD {
        public void help() {
        }
    }

    static class HelperE {
        public void help() {
        }
    }

    static class HelperF {
        public void help() {
        }
    }

    static class HelperG {
        public void help() {
        }
    }

    static class HelperH {
        public void help() {
        }
    }

    static class HelperI {
        public void help() {
        }
    }

    static class HelperJ {
        public void help() {
        }
    }

    static class HelperK {
        public void help() {
        }
    }

    static class SpaghettiClass {
        public void doWork() {
            new HelperA().help();
            new HelperB().help();
            new HelperC().help();
            new HelperD().help();
            new HelperE().help();
            new HelperF().help();
            new HelperG().help();
            new HelperH().help();
            new HelperI().help();
            new HelperJ().help();
            new HelperK().help();
        }
    }

    static class LowCohesionClass {
        private String dataA;
        private String dataB;
        private String dataC;
        private String dataD;
        private String dataE;

        public void workOnA() {
            dataA = "a";
        }

        public void workOnB() {
            dataB = "b";
        }

        public void workOnC() {
            dataC = "c";
        }

        public void workOnD() {
            dataD = "d";
        }

        public void workOnE() {
            dataE = "e";
        }
    }

    static class HighCohesionClass {
        private String name;
        private String title;

        public String getDisplay() {
            return title + " " + name;
        }

        public void setName(String n) {
            this.name = n;
        }

        public void setTitle(String t) {
            this.title = t;
        }
    }
}
