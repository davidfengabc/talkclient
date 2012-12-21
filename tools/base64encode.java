package tools;

import java.io.*;

public class base64encode {
  private static final char BASE64EN[] = {'A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z','a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z','0','1','2','3','4','5','6','7','8','9','+','/'};
  private static final int mod[] = {0,1,1};

  public static char[] base64(char[] input) {
    int sixth1;
    int sixth2;
    int sixth3;
    int sixth4;
    int thirds = input.length/3;
    int lastthird = input.length%3;
    char[] output = new char[(thirds+mod[lastthird])*4];
    int i=0;

    for(i=0; i < thirds; i++) {
      sixth1 = (input[i*3] & (0xfc)) >> 2;
      sixth2 = ((input[i*3] & (0x3)) << 4) + ((input[i*3+1] & (0xf0)) >> 4);
      sixth3 = ((input[i*3+1] & (0xf)) << 2) + ((input[i*3+2] & (0xc0)) >> 6);
      sixth4 = input[i*3+2] & (0x3f);

      output[i*4] = BASE64EN[sixth1];
      output[i*4+1] = BASE64EN[sixth2];
      output[i*4+2] = BASE64EN[sixth3];
      output[i*4+3] = BASE64EN[sixth4];
    }

    if(lastthird == 1) {
      sixth1 = (input[i*3] & (0xfc)) >> 2;
      sixth2 = (input[i*3] & (0x3)) << 4;

      output[i*4] = BASE64EN[sixth1];
      output[i*4+1] = BASE64EN[sixth2];
      output[i*4+2] = '=';
      output[i*4+3] = '=';
    } else if (lastthird == 2) {
      sixth1 = (input[i*3] & (0xfc)) >> 2;
      sixth2 = ((input[i*3] & (0x3)) << 4) + ((input[i*3+1] & (0xf0)) >> 4);
      sixth3 = (input[i*3+1] & (0xf)) << 2;

      output[i*4] = BASE64EN[sixth1];
      output[i*4+1] = BASE64EN[sixth2];
      output[i*4+2] = BASE64EN[sixth3];
      output[i*4+3] = '=';
    }

    return output;
  }

}
