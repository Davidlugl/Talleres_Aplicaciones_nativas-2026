package com.example.herbhopper_v1.navigation

import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.herbhopper_v1.data.Product
import com.example.herbhopper_v1.ui.admin.*
import com.example.herbhopper_v1.ui.admin.crud.ProductCrudScreen
import com.example.herbhopper_v1.ui.patient.*
import com.example.herbhopper_v1.ui.seller.*
import com.example.herbhopper_v1.viewmodel.CartViewModel
import com.example.herbhopper_v1.viewmodel.OrderViewModel
import com.example.herbhopper_v1.viewmodel.ProductViewModel

@Composable
fun NavGraph(navController: NavHostController) {
    val cartViewModel: CartViewModel = viewModel()
    val productViewModel: ProductViewModel = viewModel()
    val orderViewModel: OrderViewModel = viewModel()
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    val cartItems by cartViewModel.items.collectAsState()

    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {
        composable("splash") {
            SplashScreen(onTimeout = {
                navController.navigate("secure_access") {
                    popUpTo("splash") { inclusive = true }
                }
            })
        }
        // Patient Flow
        composable("secure_access") {
            SecureAccessScreen(
                onGoogleLogin = { navController.navigate("catalog") },
                onBiometricClick = { navController.navigate("login") },
                onPinClick = { navController.navigate("login") },
                onSignUpClick = { navController.navigate("create_account") },
                onForgotPasswordClick = { navController.navigate("forgot_password") },
                onAdminAccess = { navController.navigate("store_governance") },
                onSellerAccess = { navController.navigate("seller_dashboard") }
            )
        }
        composable("forgot_password") {
            ForgotPasswordScreen(onBack = { navController.popBackStack() })
        }
        composable("login") { 
            LoginScreen(
                onLoginSuccess = { navController.navigate("catalog") },
                onForgotPasswordClick = { navController.navigate("forgot_password") }
            ) 
        }
        composable("create_account") { 
            CreateAccountScreen(
                onSignUpSuccess = { navController.navigate("catalog") },
                onLoginClick = { navController.navigate("secure_access") }
            )
        }
        composable("home") { 
            MainMenuScreen(
                onLogout = { navController.navigate("secure_access") },
                onCatalogClick = { navController.navigate("catalog") },
                onOrdersClick = { navController.navigate("order_history") },
                onPrescriptionClick = { navController.navigate("prescription_validation") },
                onProfileClick = { navController.navigate("profile") }
            ) 
        }
        composable("catalog") { 
            CatalogScreen(
                productViewModel = productViewModel,
                onProductClick = { product ->
                    selectedProduct = product
                    navController.navigate("product_details")
                },
                onAddToCart = { cartViewModel.addToCart(it) },
                onMenuClick = { navController.navigate("home") },
                onCartClick = { navController.navigate("cart") },
                onHomeClick = { },
                onOrdersClick = { navController.navigate("order_history") },
                onScriptsClick = { navController.navigate("prescription_validation") },
                onProfileClick = { navController.navigate("profile") }
            )
        }
        composable("product_details") { 
            selectedProduct?.let { product ->
                ProductDetailsScreen(
                    product = product,
                    onBack = { navController.popBackStack() },
                    onAddToCart = { cartViewModel.addToCart(it) },
                    cartItemCount = cartItems.size,
                    onCartClick = { navController.navigate("cart") }
                )
            }
        }
        composable("prescription_validation") { 
            PrescriptionValidationScreen(onBack = { navController.popBackStack() }) 
        }
        composable("cart") { 
            CartScreen(
                viewModel = cartViewModel,
                onBack = { navController.popBackStack() },
                onCheckout = { navController.navigate("checkout") }
            ) 
        }
        composable("checkout") {
            CheckoutScreen(
                totalAmount = cartItems.sumOf { it.product.price * it.quantity },
                onBack = { navController.popBackStack() },
                onPaymentSuccess = {
                    cartViewModel.clearCart()
                    navController.navigate("order_history") {
                        popUpTo("cart") { inclusive = true }
                    }
                }
            )
        }
        composable("order_history") { 
            OrderHistoryScreen(
                onHomeClick = { navController.navigate("catalog") },
                onOrdersClick = { },
                onScriptsClick = { navController.navigate("prescription_validation") },
                onProfileClick = { navController.navigate("profile") }
            )
        }
        composable("order_tracking") { 
            OrderTrackingScreen(
                onHomeClick = { navController.navigate("catalog") },
                onOrdersClick = { navController.navigate("order_history") },
                onScriptsClick = { navController.navigate("prescription_validation") },
                onProfileClick = { navController.navigate("profile") }
            )
        }
        composable("profile") {
            ProfileScreen(
                onHomeClick = { navController.navigate("catalog") },
                onOrdersClick = { navController.navigate("order_history") },
                onScriptsClick = { navController.navigate("prescription_validation") },
                onLogout = { navController.navigate("secure_access") },
                onOptionClick = { option -> navController.navigate("profile_settings/$option") }
            )
        }
        composable(
            "profile_settings/{option}",
            arguments = listOf(navArgument("option") { type = NavType.StringType })
        ) { backStackEntry ->
            val option = backStackEntry.arguments?.getString("option") ?: ""
            ProfileSettingsScreen(title = option, onBack = { navController.popBackStack() })
        }
        composable("help_center") {
            HelpCenterScreen(onBack = { navController.popBackStack() })
        }

        // Seller Flow
        composable("seller_dashboard") { 
            SellerDashboardScreen(onNavigate = { navController.navigate(it) }) 
        }
        composable("inventory") { 
            InventoryScreen(
                onBack = { navController.popBackStack() },
                onNavigate = { navController.navigate(it) },
                onAddProduct = { navController.navigate("inventory_crud") },
                onEditProduct = { productId -> navController.navigate("inventory_crud/$productId") }
            ) 
        }
        composable("inventory_crud") {
            InventoryCrudScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            "inventory_crud/{productId}",
            arguments = listOf(navArgument("productId") { type = NavType.IntType })
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getInt("productId")
            InventoryCrudScreen(
                productId = productId,
                onBack = { navController.popBackStack() }
            )
        }
        composable("incoming_orders") { 
            IncomingOrdersScreen(
                onBack = { navController.popBackStack() },
                onNavigate = { navController.navigate(it) },
                viewModel = orderViewModel
            ) 
        }
        composable(
            "order_details/{orderId}",
            arguments = listOf(navArgument("orderId") { type = NavType.StringType })
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            OrderDetailsScreen(
                orderId = orderId,
                onBack = { navController.popBackStack() },
                viewModel = orderViewModel
            )
        }
        composable("store_profile") { 
            StoreProfileScreen(
                onBack = { navController.popBackStack() },
                onNavigate = { navController.navigate(it) },
                onLogout = { navController.navigate("secure_access") },
                onOptionClick = { option -> navController.navigate("profile_settings/$option") }
            ) 
        }

        // Admin Flow
        composable("store_governance") { 
            StoreGovernanceScreen(
                onNavigate = { navController.navigate(it) },
                onLogout = { navController.navigate("secure_access") }
            )
        }
        composable("user_management") {
            UserManagementScreen(onBack = { navController.popBackStack() })
        }
        composable("audit_panel") { 
            AuditPanelScreen(onNavigate = { navController.navigate(it) }) 
        }
        composable("global_analytics") { 
            GlobalAnalyticsScreen(onNavigate = { navController.navigate(it) }) 
        }
        composable("system_infrastructure") { 
            SystemInfrastructureScreen(onNavigate = { navController.navigate(it) }) 
        }
        composable("product_crud") { ProductCrudScreen() }
    }
}
