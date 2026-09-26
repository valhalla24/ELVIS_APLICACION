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

    public static boolean incluyeHora(String inicio, String fin, String horaPunto) {
        int i = aMinutos(inicio);
        int f = aMinutos(fin);
        int h = aMinutos(horaPunto);
        return h >= i && h < f;
    }

    public static List<String> generarFranjasHorarias() {
        List<String> horas = new ArrayList<>();
        for (int h = 6; h <= 22; h++) {
            int hora12 = (h > 12) ? h - 12 : h;
            if (hora12 == 0) {
                hora12 = 12;
            }
            String sufijo = (h < 12) ? "a.m." : "p.m.";
            horas.add(String.format("%02d:00 %s", hora12, sufijo));
        }
        return horas;
    }
}