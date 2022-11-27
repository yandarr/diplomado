package co.edu.iudigital.app.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import co.edu.iudigital.app.dto.UsuarioDto;
import co.edu.iudigital.app.exception.ErrorDto;
import co.edu.iudigital.app.exception.NotFoundException;
import co.edu.iudigital.app.exception.RestException;
import co.edu.iudigital.app.model.Usuario;
import co.edu.iudigital.app.service.iface.IEmailService;
import co.edu.iudigital.app.service.iface.IUsuarioService;
import co.edu.iudigital.app.util.ConstUtil;
import co.edu.iudigital.app.util.Helper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.annotations.Tag;

@RestController
@RequestMapping("/usuarios")
@Api(value = "/usuarios", tags = {"Usuarios"})
@SwaggerDefinition(tags = {
		@Tag(name = "Usuarios", description = "Gestion API Usuarios")
})
public class UsuarioController {

	@Autowired
	private IUsuarioService usuarioService;

	@Autowired
	private IEmailService emailService;
	
	@ApiOperation(value = "Obtiene todos los usuarios",
			produces = "application/json",
			httpMethod = "GET")
	@GetMapping
	public ResponseEntity<List<UsuarioDto>> index() throws RestException {
		List<UsuarioDto> usuariosDto = usuarioService.listUsers();
		return ResponseEntity.ok().body(usuariosDto);
	}
	
	@ApiOperation(value = "Obtiene un usuario por Id",
			response = Usuario.class,
			produces = "application/json",
			httpMethod = "GET")
	@GetMapping("/usuario/{id}")
	public ResponseEntity<UsuarioDto> show(@PathVariable Long id) throws RestException {
		Usuario usuario = usuarioService.listUser(id);
		UsuarioDto usuarioDto = Helper.getMapValuesClient(usuario);
		return ResponseEntity.ok().body(usuarioDto);
	}
	
	@ApiOperation(value = "Da de alta a un usuario en la app",
			response = Usuario.class,
			produces = "application/json",
			httpMethod = "POST")
	@PostMapping("/signup")
	public ResponseEntity<UsuarioDto> create(@RequestBody @Valid Usuario usuario) throws RestException{
		Usuario usuarioSaved = usuarioService.saveUser(usuario);		
		// TODO: IMPLEMENTAR SPRING SECURITY
		if(Objects.nonNull(usuarioSaved)) {
			String mess = "Su usuario "+ usuarioSaved.getUsername() +
					" y contraseña " + usuarioSaved.getPassword();
			String to = usuarioSaved.getUsername();
			String subj = ConstUtil.ASUNTO_MESSAGE;
			boolean sent = emailService.sendEmail(mess, to, subj);
			if(!sent) {
				System.out.print("No envió mensaje");
				// TODO: Colocar log y un exception
			}
		}
		UsuarioDto usuarioDto = Helper.getMapValuesClient(usuarioSaved);
		return ResponseEntity.status(HttpStatus.CREATED).body(usuarioDto);
	}
	
	@PostMapping("/upload/{email}")//TODO: cambiar a spring security
	public ResponseEntity<?> upload(
			@RequestParam("image") MultipartFile image,
			@PathVariable String email) throws RestException{
		Map<String, Object> response = new HashMap<>();
		Usuario usuario = usuarioService.listByUsername(email);
		if(!image.isEmpty()) {
			String nombreImage = UUID.randomUUID().toString()
					.concat("_")
					.concat(image.getOriginalFilename().replace(" ", ""));
			Path path = Paths.get("uploads").resolve(nombreImage).toAbsolutePath();
			// uploads/asdasdads_mifoto.jpg
			try {
				Files.copy(image.getInputStream(), path);
			} catch (IOException e) {
				response.put("Error IO", e.getMessage().concat(e.getCause().getMessage()));
				return new ResponseEntity<Map<String, Object>>(response, HttpStatus.INTERNAL_SERVER_ERROR);
			}
			String imageBD = usuario.getImage();
			// borrar imagen vieja
			if(Objects.nonNull(imageBD) && imageBD.length() > 0
					&& !imageBD.startsWith("http")) {
				Path pathAntes = Paths.get("uploads").resolve(imageBD).toAbsolutePath();
				// uploads/iidsadasda_1.jpg
				File imageFileAntes = pathAntes.toFile();
				if(imageFileAntes.exists() && imageFileAntes.canRead()) {
					imageFileAntes.delete();
				}
			}
			usuario.setImage(nombreImage);
			usuarioService.updateUser(usuario);
			response.put("usuario", usuario);
		}
		return new ResponseEntity<Map<String, Object>>(response, HttpStatus.CREATED);
	}
	
	@ApiOperation(value = "Actualiza un usuario",
			response = Usuario.class,
			produces = "application/json",
			httpMethod = "PUT")
	@PutMapping("/usuario")
	public ResponseEntity<Usuario> update(@PathVariable String username, 
			@RequestBody Usuario usuario) throws RestException{
		Usuario usuarioBD = usuarioService.listByUsername(username);
		if(Objects.isNull(usuarioBD)) {
			throw new NotFoundException(ErrorDto.getErrorDto(
					HttpStatus.NOT_FOUND.getReasonPhrase(), 
					ConstUtil.MESSAGE_NOT_FOUND, 
					HttpStatus.NOT_FOUND.value()
					)
				);
		}
		usuarioBD.setNombre(usuario.getNombre());
		usuarioBD.setApellido(usuario.getApellido());
		usuarioBD.setFechaNacimiento(usuario.getFechaNacimiento());
		usuarioBD.setPassword(usuario.getPassword()); // TODO: implementar con Spring security
		return ResponseEntity.status(HttpStatus.CREATED).body(
				usuarioService.updateUser(usuarioBD)
				);
	}
}
