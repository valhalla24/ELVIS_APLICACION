package co.edu.usc.elvis.util;

import java.util.ArrayList;
import java.util.List;

public class HorarioUtil {

    private HorarioUtil() {
    }

    public static int aMinutos(String horaTexto) {
        String texto = horaTexto.trim().toLowerCase();
        boolean esPM = texto.contains("p.m.") || texto.contains("pm");
        String soloNumeros = texto.replaceAll("[^0-9:]", "");
        String[] partes = soloNumeros.split(":");
        int hora = Integer.parseInt(partes[0]);
        int minuto = partes.length > 1 ? Integer.parseInt(partes[1]) : 0;
        if (esPM && hora != 12) {
            hora += 12;
        }
        if (!esPM && hora == 12) {
            hora = 0;
        }
        return hora * 60 + minuto;
    }

    public static boolean seSuperponen(String inicio1, String fin1, String inicio2, String fin2) {
        int i1 = aMinutos(inicio1);
        int f1 = aMinutos(fin1);
        int i2 = aMinutos(inicio2);
        int f2 = aMinutos(fin2);
        return i1 < f2 && i2 < f1;
    }

    /** True si horaPunto cae dentro de [inicio, fin). */
    public static boolean incluyeHora(String inicio, String fin, String horaPunto) {
        int i = aMinutos(inicio);
        int f = aMinutos(fin);
        int h = aMinutos(horaPunto);
        return h >= i && h < f;
    }

    /** Franjas de media hora, de 06:00 a.m. a 10:00 p.m. (se usan en el calendario y en los horarios de los cursos). */
    public static List<String> generarFranjasHorarias() {
        List<String> horas = new ArrayList<>();
        for (int minutos = 6 * 60; minutos <= 22 * 60; minutos += 30) {
            int hora24 = minutos / 60;
            int minuto = minutos % 60;
            int hora12 = hora24 > 12 ? hora24 - 12 : hora24;
            if (hora12 == 0) {
                hora12 = 12;
            }
            String sufijo = hora24 < 12 ? "a.m." : "p.m.";
            horas.add(String.format("%02d:%02d %s", hora12, minuto, sufijo));
        }
        return horas;
    }
}